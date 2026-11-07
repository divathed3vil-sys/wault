package com.wault.app.data

import com.wault.app.domain.engine.PairingStage
import com.wault.app.domain.engine.Receipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 1 state holder: plain in-memory StateFlows. Room + DataStore land in
 * Phase 2; the repository API is already shaped so the swap is invisible to
 * the UI.
 */
class InMemoryStore {

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messagesByChat = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesByChat: StateFlow<Map<String, List<Message>>> = _messagesByChat.asStateFlow()

    private val _presence = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presence: StateFlow<Map<String, Boolean>> = _presence.asStateFlow()

    private val _pairingStages = MutableStateFlow<Map<String, PairingStage>>(emptyMap())
    val pairingStages: StateFlow<Map<String, PairingStage>> = _pairingStages.asStateFlow()

    // ------------------------------------------------------------------ //
    // Seeding                                                             //
    // ------------------------------------------------------------------ //

    fun seedAccounts(seed: List<Account>) {
        _accounts.value = seed
    }

    fun seedChats(seed: List<Chat>) {
        _chats.value = seed
    }

    fun seedMessages(seed: Map<String, List<Message>>) {
        _messagesByChat.value = seed
    }

    // ------------------------------------------------------------------ //
    // Accounts                                                            //
    // ------------------------------------------------------------------ //

    fun addAccount(account: Account) {
        _accounts.update { current -> current + account }
    }

    fun updateAccountStatus(accountId: String, status: AccountStatus) {
        _accounts.update { list -> list.map { if (it.id == accountId) it.copy(status = status) else it } }
    }

    fun incrementAccountUnread(accountId: String) {
        _accounts.update { list ->
            list.map { if (it.id == accountId) it.copy(unreadTotal = it.unreadTotal + 1) else it }
        }
    }

    fun setAccountUnread(accountId: String, unread: Int) {
        _accounts.update { list ->
            list.map { if (it.id == accountId) it.copy(unreadTotal = unread) else it }
        }
    }

    // ------------------------------------------------------------------ //
    // Chats                                                               //
    // ------------------------------------------------------------------ //

    fun upsertChat(chat: Chat) {
        _chats.update { list ->
            val index = list.indexOfFirst { it.id == chat.id }
            if (index >= 0) list.toMutableList().apply { this[index] = chat } else list + chat
        }
    }

    /** Snippet/lastTs/unread progression when a message lands on a chat. */
    fun bumpChat(chatId: String, snippet: String, lastTs: Long, incrementUnread: Boolean) {
        _chats.update { list ->
            list.map {
                if (it.id == chatId) {
                    it.copy(
                        snippet = snippet,
                        lastTs = lastTs,
                        unread = if (incrementUnread) it.unread + 1 else it.unread,
                    )
                } else {
                    it
                }
            }
        }
    }

    fun setChatUnread(chatId: String, unread: Int) {
        _chats.update { list ->
            list.map { if (it.id == chatId) it.copy(unread = unread) else it }
        }
    }

    // ------------------------------------------------------------------ //
    // Messages                                                            //
    // ------------------------------------------------------------------ //

    fun appendMessage(message: Message) {
        _messagesByChat.update { map ->
            val current = map[message.chatId].orEmpty()
            // Stable sort keeps equal-timestamp messages in arrival order.
            map + (message.chatId to (current + message).sortedBy { it.timestampEpochMs })
        }
    }

    fun updateReceipt(chatId: String, messageId: String, receipt: Receipt) {
        _messagesByChat.update { map ->
            val current = map[chatId].orEmpty()
            if (current.none { it.id == messageId }) return@update map
            map + (chatId to current.map { if (it.id == messageId) it.copy(receipt = receipt) else it })
        }
    }

    // ------------------------------------------------------------------ //
    // Presence & pairing                                                  //
    // ------------------------------------------------------------------ //

    fun setPresence(chatId: String, online: Boolean) {
        _presence.update { it + (chatId to online) }
    }

    fun setPairingStage(accountId: String, stage: PairingStage) {
        _pairingStages.update { it + (accountId to stage) }
    }

    // ------------------------------------------------------------------ //
    // Read helpers (snapshot semantics, fine for the demo scale)          //
    // ------------------------------------------------------------------ //

    fun accountById(id: String): Account? = _accounts.value.firstOrNull { it.id == id }

    fun chatById(id: String): Chat? = _chats.value.firstOrNull { it.id == id }
}
