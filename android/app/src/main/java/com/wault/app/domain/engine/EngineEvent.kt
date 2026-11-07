package com.wault.app.domain.engine

/**
 * The single event contract every [SessionEngine] speaks.
 *
 * The UI never knows which engine is live (mock, whatsmeow, bridge); it only
 * reacts to this stream. Swapping engines changes zero UI code.
 */
sealed interface EngineEvent {

    /** A message arrived — or, for outgoing sends, was echoed back by the engine. */
    data class MessageReceived(
        val accountId: String,
        val chatId: String,
        val messageId: String,
        val senderName: String,
        val text: String,
        val isFromMe: Boolean,
        val timestampEpochMs: Long,
    ) : EngineEvent

    /** Delivery state of a previously seen message advanced. */
    data class ReceiptUpdated(
        val accountId: String,
        val chatId: String,
        val messageId: String,
        val receipt: Receipt,
    ) : EngineEvent

    /** Full snapshot of one chat's list-row state; upsert semantics. */
    data class ChatChanged(
        val accountId: String,
        val chat: ChatSnapshot,
    ) : EngineEvent

    /** Contact presence toggle for a chat (used for the 1:1 subtitle). */
    data class PresenceChanged(
        val accountId: String,
        val chatId: String,
        val online: Boolean,
    ) : EngineEvent

    /** QR pairing state machine progression. */
    data class PairingUpdate(
        val accountId: String,
        val stage: PairingStage,
    ) : EngineEvent

    /** Non-fatal engine failure (send on an unlinked session, protocol hiccup, ...). */
    data class EngineError(
        val accountId: String,
        val message: String,
    ) : EngineEvent
}

/** Outgoing message delivery state: one tick, two ticks, two accent ticks. */
enum class Receipt { Sent, Delivered, Read }

/** QR pairing progression: Waiting -> QrReady -> Linking -> Linked (or Failed). */
enum class PairingStage { Waiting, QrReady, Linking, Linked, Failed }

/**
 * Engine-side snapshot of a chat's row state. The data layer maps this onto
 * the UI [com.wault.app.data.Chat] model, filling in account identity.
 */
data class ChatSnapshot(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val snippet: String,
    val lastTsEpochMs: Long,
    val unread: Int,
)
