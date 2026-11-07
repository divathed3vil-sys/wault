package com.wault.app.domain.engine

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * Per-account simulated WhatsApp session.
 *
 * Behaviour mirrors the product, not the protocol:
 *  - Linked accounts emit a [EngineEvent.ChatChanged] snapshot per seeded chat
 *    on [start], then live: an ambient incoming message every 45-90 s, a
 *    presence toggle for 1:1 chats every 25-50 s, and on [send] a receipt
 *    walk Sent (~0.8 s) -> Delivered (~1.8 s) -> Read (~2.1 s) followed by a
 *    contextual canned reply ~2.5-4 s after the send.
 *  - Unlinked accounts run the pairing state machine: Waiting -> QrReady
 *    (~1.5 s), and the caller advances QrReady -> Linking -> Linked by calling
 *    [confirmQrScanned] (the mock stand-in for the camera scanning the QR).
 *
 * All delays run on an internal scope that [stop] cancels. Events are posted
 * into a [MutableSharedFlow] with a 64-slot buffer and no replay — the
 * repository seeds the initial store state directly, so late collectors are
 * expected and safe.
 */
class MockEngine(
    override val accountId: String,
    private val chatSeeds: List<ChatSeed>,
    private val linked: Boolean,
) : SessionEngine {

    /**
     * Minimal chat description the engine needs to emit snapshots and pick
     * plausible senders. Seeded from the mirrored demo dataset.
     */
    data class ChatSeed(
        val chatId: String,
        val name: String,
        val isGroup: Boolean,
        val snippet: String,
        val lastTsEpochMs: Long,
        val unread: Int,
        val members: List<String> = emptyList(),
    )

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("MockEngine:$accountId"),
    )
    private val _events = MutableSharedFlow<EngineEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<EngineEvent> = _events
    override val kind: EngineKind = EngineKind.Mock

    @Volatile
    private var started = false

    private var pairingStage: PairingStage? = if (linked) null else PairingStage.Waiting

    override fun start() {
        if (started) return
        started = true
        if (linked) {
            scope.launch {
                chatSeeds.forEach { seed -> post(EngineEvent.ChatChanged(accountId, seed.toSnapshot())) }
            }
            scope.launch { ambientLoop() }
            scope.launch { presenceLoop() }
        } else {
            scope.launch {
                post(EngineEvent.PairingUpdate(accountId, PairingStage.Waiting))
                delay(QR_READY_DELAY_MS)
                pairingStage = PairingStage.QrReady
                post(EngineEvent.PairingUpdate(accountId, PairingStage.QrReady))
            }
        }
    }

    override fun stop() {
        started = false
        scope.cancel()
    }

    override suspend fun send(chatId: String, text: String): Boolean {
        if (!linked) {
            post(EngineEvent.EngineError(accountId, "Session is not linked yet"))
            return false
        }
        val messageId = newMessageId()
        post(
            EngineEvent.MessageReceived(
                accountId = accountId,
                chatId = chatId,
                messageId = messageId,
                senderName = YOU_LABEL,
                text = text,
                isFromMe = true,
                timestampEpochMs = System.currentTimeMillis(),
            ),
        )
        scope.launch { deliverySequence(chatId, messageId, text) }
        return true
    }

    override suspend fun markRead(chatId: String) {
        // A real engine pushes a read receipt upstream; the mock is a no-op.
    }

    /**
     * Advance the pairing simulation: QrReady -> Linking -> Linked.
     * Ignored unless the engine is currently showing a ready QR.
     */
    fun confirmQrScanned() {
        if (pairingStage != PairingStage.QrReady) return
        pairingStage = PairingStage.Linking
        post(EngineEvent.PairingUpdate(accountId, PairingStage.Linking))
        scope.launch {
            delay(LINKING_DELAY_MS)
            pairingStage = PairingStage.Linked
            post(EngineEvent.PairingUpdate(accountId, PairingStage.Linked))
        }
    }

    // ------------------------------------------------------------------ //
    // Life simulation                                                     //
    // ------------------------------------------------------------------ //

    private suspend fun deliverySequence(chatId: String, messageId: String, sentText: String) {
        val seed = chatSeeds.firstOrNull { it.chatId == chatId }
        post(EngineEvent.ReceiptUpdated(accountId, chatId, messageId, Receipt.Sent))
        delay(SENT_DELAY_MS)
        post(EngineEvent.ReceiptUpdated(accountId, chatId, messageId, Receipt.Delivered))
        delay(DELIVERED_DELAY_MS)
        post(EngineEvent.ReceiptUpdated(accountId, chatId, messageId, Receipt.Read))
        delay(random.nextLong(REPLY_MIN_DELAY_MS, REPLY_MAX_DELAY_MS))
        post(cannedReply(chatId, seed, sentText))
    }

    private suspend fun ambientLoop() {
        while (started && coroutineContext.isActive) {
            delay(random.nextLong(AMBIENT_MIN_DELAY_MS, AMBIENT_MAX_DELAY_MS))
            val seed = chatSeeds.randomOrNull(random) ?: continue
            val sender = if (seed.isGroup) {
                seed.members.randomOrNull(random) ?: seed.name
            } else {
                seed.name
            }
            post(
                EngineEvent.MessageReceived(
                    accountId = accountId,
                    chatId = seed.chatId,
                    messageId = newMessageId(),
                    senderName = sender,
                    text = AMBIENT_TEXTS.random(random),
                    isFromMe = false,
                    timestampEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private suspend fun presenceLoop() {
        val directChats = chatSeeds.filter { !it.isGroup }
        if (directChats.isEmpty()) return
        while (started && coroutineContext.isActive) {
            delay(random.nextLong(PRESENCE_MIN_DELAY_MS, PRESENCE_MAX_DELAY_MS))
            val chat = directChats.random(random)
            post(EngineEvent.PresenceChanged(accountId, chat.chatId, random.nextBoolean()))
        }
    }

    private fun cannedReply(chatId: String, seed: ChatSeed?, sentText: String): EngineEvent.MessageReceived {
        val sender = when {
            seed == null -> "Contact"
            seed.isGroup -> seed.members.randomOrNull(random) ?: seed.name
            else -> seed.name
        }
        return EngineEvent.MessageReceived(
            accountId = accountId,
            chatId = chatId,
            messageId = newMessageId(),
            senderName = sender,
            text = replyFor(sentText),
            isFromMe = false,
            timestampEpochMs = System.currentTimeMillis(),
        )
    }

    /** Keyword-shaped replies that echo the web demo's conversational tone. */
    private fun replyFor(sent: String): String = when {
        sent.contains("?", ignoreCase = true) -> "Yes, that works on my side"
        sent.contains("invoice", ignoreCase = true) || sent.contains("pdf", ignoreCase = true) ->
            "Sure, sharing the invoice now"
        sent.contains("deliver", ignoreCase = true) -> "Noted, the driver just left the warehouse"
        sent.contains("order", ignoreCase = true) -> "Thanks, I will confirm the order shortly"
        sent.contains("address", ignoreCase = true) -> "Sending our address in a moment"
        sent.contains("price", ignoreCase = true) || sent.contains("rate", ignoreCase = true) ->
            "I will check the latest price and update you"
        else -> GENERIC_REPLIES.random(random)
    }

    private fun ChatSeed.toSnapshot() = ChatSnapshot(
        id = chatId,
        name = name,
        isGroup = isGroup,
        snippet = snippet,
        lastTsEpochMs = lastTsEpochMs,
        unread = unread,
    )

    private fun newMessageId(): String = "live-${random.nextLong()}"

    private fun post(event: EngineEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val YOU_LABEL = "You"
        const val QR_READY_DELAY_MS = 1_500L
        const val LINKING_DELAY_MS = 1_200L
        const val SENT_DELAY_MS = 800L
        const val DELIVERED_DELAY_MS = 1_000L
        const val READ_DELAY_MS = 300L
        const val REPLY_MIN_DELAY_MS = 400L
        const val REPLY_MAX_DELAY_MS = 1_800L
        const val AMBIENT_MIN_DELAY_MS = 45_000L
        const val AMBIENT_MAX_DELAY_MS = 90_000L
        const val PRESENCE_MIN_DELAY_MS = 25_000L
        const val PRESENCE_MAX_DELAY_MS = 50_000L

        val GENERIC_REPLIES = listOf(
            "Got it, thanks",
            "Noted, thank you",
            "Sure, I will follow up",
            "Ok, I will check and update you",
            "Thanks, appreciate it",
        )

        val AMBIENT_TEXTS = listOf(
            "Can we move the call to 4 pm?",
            "The courier just arrived at the shop",
            "Sharing the updated list in a bit",
            "Is Friday still ok for the delivery?",
            "Quick update: the van left Kandy",
            "Please send the waybill when you can",
            "Payment will go out today",
        )
    }
}
