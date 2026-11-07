package com.wault.app.domain

import com.wault.app.di.ApplicationScope
import com.wault.app.domain.engine.EngineEvent
import com.wault.app.domain.engine.SessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Inbox Bus: fans every registered engine's event stream into one
 * ordered [SharedFlow] that the repository collects.
 *
 * Engines register at runtime (bootstrap, pairing, later Phase 3 restarts);
 * each registration re-merges the live set via [flatMapLatest]. The shared
 * flow keeps a small replay window so the single repository collector never
 * misses bootstrap emissions while the merge pipeline spins up — consumers
 * must therefore tolerate a short replay (the demo dataset seeding is
 * idempotent, so replays are harmless upserts).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class InboxBus @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val engines = MutableStateFlow<List<SessionEngine>>(emptyList())

    private val _events = MutableSharedFlow<EngineEvent>(
        replay = REPLAY,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<EngineEvent> = _events

    init {
        scope.launch {
            engines
                .flatMapLatest { list: List<SessionEngine> ->
                    merge(*list.map { it.events }.toTypedArray())
                }
                .collect { event -> _events.tryEmit(event) }
        }
    }

    /** Attaches an engine to the bus. Register before calling [SessionEngine.start]. */
    fun register(engine: SessionEngine) {
        engines.update { current ->
            if (current.any { it.accountId == engine.accountId }) current else current + engine
        }
    }

    fun registered(): List<SessionEngine> = engines.value

    private companion object {
        const val REPLAY = 64
    }
}
