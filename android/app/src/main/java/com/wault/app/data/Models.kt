package com.wault.app.data

import com.wault.app.domain.engine.Receipt

/** Lifecycle of one linked WhatsApp account as shown on the Vault. */
enum class AccountStatus { Connected, Connecting, Qr, Reconnecting, Disconnected, Error }

/** One WhatsApp number Wault holds a session for. */
data class Account(
    val id: String,
    val name: String,
    val accentHex: String,
    val phone: String,
    val status: AccountStatus,
    val unreadTotal: Int,
)

/**
 * One conversation row. [id] is the composite "accountId|chatJid" key so a
 * chat is globally addressable across accounts; [accentHex] carries the
 * owning account's accent for tinting.
 */
data class Chat(
    val id: String,
    val accountId: String,
    val name: String,
    val isGroup: Boolean,
    val snippet: String,
    val lastTs: Long,
    val unread: Int,
    val accentHex: String,
)

enum class MessageKind { Text, Media }

/** One chat message. [receipt] is only ever set on outgoing messages. */
data class Message(
    val id: String,
    val chatId: String,
    val senderName: String,
    val text: String,
    val isFromMe: Boolean,
    val timestampEpochMs: Long,
    val receipt: Receipt? = null,
    val kind: MessageKind = MessageKind.Text,
)
