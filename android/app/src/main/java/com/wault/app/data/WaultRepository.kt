package com.wault.app.data

import android.util.Log
import com.wault.app.di.ApplicationScope
import com.wault.app.domain.InboxBus
import com.wault.app.domain.SessionDirector
import com.wault.app.domain.engine.EngineEvent
import com.wault.app.domain.engine.MockEngine
import com.wault.app.domain.engine.PairingStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Facade the ViewModels talk to. Wires InboxBus events into store mutations
 * (snippets, unread counts, receipt progression, message appends, pairing
 * stages) and exposes the read-side StateFlows plus the small command surface
 * the screens need.
 *
 * The demo dataset mirrors `web/src/components/wault/data.ts`, so snippet
 * conventions match the web edition exactly: outgoing last messages are
 * prefixed "You: ", group messages "Sender: ".
 */
class WaultRepository(
    private val store: InMemoryStore,
    private val bus: InboxBus,
    private val director: SessionDirector,
    @ApplicationScope private val scope: CoroutineScope,
) {

    init {
        scope.launch { bus.events.collect { handleEvent(it) } }
    }

    val accounts: StateFlow<List<Account>> get() = store.accounts
    val chats: StateFlow<List<Chat>> get() = store.chats
    val messagesByChat: StateFlow<Map<String, List<Message>>> get() = store.messagesByChat
    val presence: StateFlow<Map<String, Boolean>> get() = store.presence
    val pairingStages: StateFlow<Map<String, PairingStage>> get() = store.pairingStages

    /** Seeds the store from the mirrored demo dataset. Idempotent. */
    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        store.seedAccounts(
            DemoDataset.accounts.map { account ->
                Account(
                    id = account.id,
                    name = account.name,
                    accentHex = account.accentHex,
                    phone = account.phone,
                    status = account.status,
                    unreadTotal = account.unreadTotal,
                )
            },
        )
        store.seedChats(DemoDataset.chats.map { demo -> demo.toChat() })
        store.seedMessages(DemoDataset.conversationsByChat())
    }

    fun messages(chatId: String): List<Message> = store.messagesByChat.value[chatId].orEmpty()

    fun send(chatId: String, text: String) {
        val accountId = accountIdOf(chatId) ?: return
        val engine = director.engineFor(accountId) ?: return
        scope.launch { engine.send(chatId, text) }
    }

    fun markRead(chatId: String) {
        store.setChatUnread(chatId, 0)
        recomputeAccountUnread(chatId)
        val accountId = accountIdOf(chatId) ?: return
        val engine = director.engineFor(accountId) ?: return
        scope.launch { engine.markRead(chatId) }
    }

    /** Creates a pairing-mode account + engine and returns the accountId. */
    fun startPairing(name: String, accentHex: String): String {
        val id = director.startPairing(name, accentHex)
        store.addAccount(
            Account(
                id = id,
                name = name,
                accentHex = accentHex,
                phone = "",
                status = AccountStatus.Connecting,
                unreadTotal = 0,
            ),
        )
        return id
    }

    /** Advances the mock QR flow after the user "scans" the placeholder code. */
    fun confirmPairingScan(accountId: String) {
        (director.engineFor(accountId) as? MockEngine)?.confirmQrScanned()
    }

    fun retryPairing(accountId: String) {
        director.retryPairing(accountId)
    }

    // ------------------------------------------------------------------ //
    // Event handling                                                      //
    // ------------------------------------------------------------------ //

    private fun handleEvent(event: EngineEvent) {
        when (event) {
            is EngineEvent.MessageReceived -> onMessage(event)
            is EngineEvent.ReceiptUpdated ->
                store.updateReceipt(event.chatId, event.messageId, event.receipt)
            is EngineEvent.ChatChanged -> {
                val accent = store.accountById(event.accountId)?.accentHex ?: DemoDataset.FALLBACK_ACCENT
                store.upsertChat(
                    Chat(
                        id = event.chat.id,
                        accountId = event.accountId,
                        name = event.chat.name,
                        isGroup = event.chat.isGroup,
                        snippet = event.chat.snippet,
                        lastTs = event.chat.lastTsEpochMs,
                        unread = event.chat.unread,
                        accentHex = accent,
                    ),
                )
                recomputeAccountUnread(event.chat.id)
            }
            is EngineEvent.PairingUpdate -> {
                store.setPairingStage(event.accountId, event.stage)
                store.updateAccountStatus(event.accountId, event.stage.toAccountStatus())
            }
            is EngineEvent.PresenceChanged -> store.setPresence(event.chatId, event.online)
            is EngineEvent.EngineError -> Log.w(TAG, "engine ${event.accountId}: ${event.message}")
        }
    }

    private fun onMessage(event: EngineEvent.MessageReceived) {
        val chat = store.chatById(event.chatId)
        store.appendMessage(
            Message(
                id = event.messageId,
                chatId = event.chatId,
                senderName = event.senderName,
                text = event.text,
                isFromMe = event.isFromMe,
                timestampEpochMs = event.timestampEpochMs,
                receipt = null,
                kind = MessageKind.Text,
            ),
        )
        val snippet = when {
            event.isFromMe -> "You: ${event.text}"
            chat?.isGroup == true -> "${event.senderName}: ${event.text}"
            else -> event.text
        }
        store.bumpChat(
            chatId = event.chatId,
            snippet = snippet,
            lastTs = event.timestampEpochMs,
            incrementUnread = !event.isFromMe,
        )
        if (!event.isFromMe) {
            chat?.accountId?.let(store::incrementAccountUnread)
        }
    }

    private fun recomputeAccountUnread(chatId: String) {
        val chat = store.chatById(chatId) ?: return
        val total = store.chats.value
            .filter { it.accountId == chat.accountId }
            .sumOf { it.unread }
        store.setAccountUnread(chat.accountId, total)
    }

    private fun accountIdOf(chatId: String): String? =
        chatId.substringBefore(CHAT_ID_DELIMITER, missingDelimiterValue = "").takeIf { it.isNotEmpty() }

    private fun PairingStage.toAccountStatus(): AccountStatus = when (this) {
        PairingStage.Waiting -> AccountStatus.Connecting
        PairingStage.QrReady -> AccountStatus.Qr
        PairingStage.Linking -> AccountStatus.Connecting
        PairingStage.Linked -> AccountStatus.Connected
        PairingStage.Failed -> AccountStatus.Error
    }

    private fun DemoDataset.DemoChat.toChat(): Chat = Chat(
        id = DemoDataset.compositeId(accountId, id),
        accountId = accountId,
        name = name,
        isGroup = isGroup,
        snippet = snippet,
        lastTs = DemoDataset.epochForMinutes(minutesOfDay, yesterday),
        unread = unread,
        accentHex = DemoDataset.accountById(accountId)?.accentHex ?: DemoDataset.FALLBACK_ACCENT,
    )

    private companion object {
        const val TAG = "WaultRepository"
        const val CHAT_ID_DELIMITER = "|"
    }
}
