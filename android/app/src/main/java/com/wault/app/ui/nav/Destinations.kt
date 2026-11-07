package com.wault.app.ui.nav

import android.net.Uri

/**
 * Route constants for the three tabs plus the two full-screen destinations.
 * Chat ids are composite ("a1|c1") and get Uri-encoded inside path segments.
 */
object WaultDestinations {
    const val VAULT = "vault"
    const val INBOX_PATTERN = "inbox?filter={filter}"
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val CONVERSATION_PATTERN = "conversation/{chatId}"
    const val PAIRING_PATTERN = "pairing/{accountId}"

    fun inbox(filter: String): String = "inbox?filter=$filter"

    fun conversation(chatId: String): String = "conversation/" + Uri.encode(chatId)

    fun pairing(accountId: String): String = "pairing/" + Uri.encode(accountId)
}
