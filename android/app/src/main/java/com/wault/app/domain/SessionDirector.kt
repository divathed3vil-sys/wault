package com.wault.app.domain

import com.wault.app.data.DemoDataset
import com.wault.app.domain.engine.EngineKind
import com.wault.app.domain.engine.MockEngine
import com.wault.app.domain.engine.SessionEngine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the accountId -> [SessionEngine] map and the lifecycle of every session.
 *
 * Phase 1 keeps all sessions HOT: created at app start, alive until
 * [stopAll]. Warm/cold tiering, device-tier policies and the per-account
 * event journal (the MemoryDirector concept, reborn for the protocol era)
 * arrive with SessionDirector v2 in Phase 3.
 */
@Singleton
class SessionDirector @Inject constructor(
    private val bus: InboxBus,
) {

    private val engines = ConcurrentHashMap<String, SessionEngine>()
    private val bootstrapped = AtomicBoolean(false)
    private val pairingCounter = AtomicInteger(DemoDataset.accounts.size)

    /**
     * Creates and starts hot [MockEngine] sessions for the demo accounts
     * mirrored from the web demo dataset. Idempotent.
     */
    fun bootstrapDemoAccounts() {
        if (!bootstrapped.compareAndSet(false, true)) return
        DemoDataset.accounts.forEach { account ->
            attach(
                MockEngine(
                    accountId = account.id,
                    chatSeeds = DemoDataset.chatSeedsFor(account.id),
                    linked = true,
                ),
            )
        }
    }

    /**
     * Creates a new account in pairing mode (unlinked MockEngine running the
     * QR state machine) and returns its accountId.
     *
     * [name] and [accentHex] are accepted now so the Phase 2 real engine can
     * receive the chosen identity directly; in Phase 1 the repository records
     * them on the Account model.
     */
    fun startPairing(name: String, accentHex: String): String {
        val id = "a${pairingCounter.incrementAndGet()}"
        attach(MockEngine(accountId = id, chatSeeds = emptyList(), linked = false))
        return id
    }

    /**
     * Replaces a dead or failed pairing session with a fresh one for the same
     * accountId. Used by the pairing screen's retry affordance.
     */
    fun retryPairing(accountId: String): Boolean {
        val existing = engines.remove(accountId) ?: return false
        existing.stop()
        attach(MockEngine(accountId = accountId, chatSeeds = emptyList(), linked = false))
        return true
    }

    fun engineFor(accountId: String): SessionEngine? = engines[accountId]

    fun engineKinds(): Map<String, EngineKind> =
        engines.entries.associate { it.key to it.value.kind }

    fun stopAll() {
        engines.values.forEach { it.stop() }
        engines.clear()
        bootstrapped.set(false)
    }

    private fun attach(engine: SessionEngine) {
        engines[engine.accountId] = engine
        bus.register(engine)
        engine.start()
    }
}
