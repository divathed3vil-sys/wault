package com.wault.app.domain.engine

import kotlinx.coroutines.flow.SharedFlow

/**
 * A live session with one WhatsApp account.
 *
 * The UI speaks only to this interface. Phase 1 ships [MockEngine]; Phase 2
 * adds a whatsmeow-backed engine (Go compiled to an .aar via gomobile) that
 * implements the exact same contract, and Phase 4 may add a BridgeEngine for
 * office mode. None of those swaps touch a single screen.
 */
interface SessionEngine {

    val accountId: String

    /**
     * Hot event stream. No replay: collectors that arrive late see only events
     * emitted from that point on (the repository seeds initial state directly).
     */
    val events: SharedFlow<EngineEvent>

    fun start()

    fun stop()

    /**
     * Optimistic send. Returns false when the session cannot send right now
     * (unlinked, stopped); the engine echoes the message back via
     * [EngineEvent.MessageReceived] and then walks its receipts forward.
     */
    suspend fun send(chatId: String, text: String): Boolean

    /** Acknowledge that the user has read everything in [chatId]. */
    suspend fun markRead(chatId: String)

    /** Which protocol implementation backs this session. */
    val kind: EngineKind
}

/** Which protocol implementation backs a session. */
enum class EngineKind {

    /** In-process simulator used for UI development (Phase 1). */
    Mock,

    /**
     * Real WhatsApp multi-device protocol via whatsmeow. Phase 2 placeholder —
     * the WhatsmeowEngine implementation lands behind this same interface.
     */
    WhatsApp,
}
