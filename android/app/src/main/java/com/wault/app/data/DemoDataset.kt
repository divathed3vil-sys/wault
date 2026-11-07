package com.wault.app.data

import com.wault.app.domain.engine.MockEngine.ChatSeed
import com.wault.app.domain.engine.Receipt
import java.time.LocalDate
import java.time.ZoneId

/**
 * Demo dataset mirrored 1:1 from `web/src/components/wault/data.ts` so the
 * web demo and the Android demo feel like the same product: same accounts
 * (Sales · Aadhil, Support · Nethmi, Ops · Dilan), same chats, same
 * conversation scripts, same snippet conventions ("You: ", "Kasun: ...").
 */
object DemoDataset {

    /** WhatsApp allows 4 linked devices per account — the Vault slot meter. */
    const val DEVICE_SLOTS = 4

    /** Default label for freshly paired demo numbers. */
    const val DEFAULT_PAIRING_NAME = "New number"

    /** Curated account accent palette (same hexes as the web edition). */
    val ACCENT_HEXES = listOf("#10B981", "#F59E0B", "#F43F5E", "#8B5CF6", "#14B8A6")

    const val FALLBACK_ACCENT = "#10B981"

    data class DemoAccount(
        val id: String,
        val name: String,
        val phone: String,
        val accentHex: String,
        val status: AccountStatus,
        val unreadTotal: Int,
    )

    data class DemoChat(
        val id: String,
        val accountId: String,
        val name: String,
        val isGroup: Boolean,
        val snippet: String,
        val minutesOfDay: Int,
        val unread: Int,
        val yesterday: Boolean = false,
        val members: List<String> = emptyList(),
    )

    data class DemoMessage(
        val id: String,
        val direction: String, // "in" | "out" — mirrors data.ts
        val minutesOfDay: Int,
        val kind: String, // "text" | "media"
        val text: String,
        val delivery: String? = null, // "sent" | "read"
    )

    val accounts = listOf(
        DemoAccount("a1", "Sales · Aadhil", "+94 77 214 8890", "#10B981", AccountStatus.Connected, 9),
        DemoAccount("a2", "Support · Nethmi", "+94 76 880 1123", "#F43F5E", AccountStatus.Connected, 4),
        DemoAccount("a3", "Ops · Dilan", "+94 71 553 2047", "#F59E0B", AccountStatus.Reconnecting, 2),
    )

    val chats = listOf(
        DemoChat("c1", "a1", "Malith Fernando", false, "Sure, I'll confirm the order by 4 pm", 16 * 60 + 5, 3),
        DemoChat("c2", "a2", "Hashini Perera", false, "The replacement unit was delivered, thank you", 15 * 60 + 52, 1),
        DemoChat(
            "c3", "a1", "Ceylon Traders", true, "Kasun: Van left the Kandy warehouse", 15 * 60 + 40, 2,
            members = listOf("Kasun", "Chamil", "Fathima"),
        ),
        DemoChat("c4", "a2", "Tharindu Silva", false, "You: Can you share the invoice PDF again?", 15 * 60 + 21, 0),
        DemoChat("c5", "a3", "Nadeesha Wickrama", false, "Schedule changed to Tuesday 10 am", 14 * 60 + 48, 2),
        DemoChat("c6", "a1", "Ruwan Jayasuriya", false, "You: Sent the catalogue. Let me know", 13 * 60 + 30, 0),
        DemoChat("c7", "a2", "Sanduni Bandara", false, "Thank you, that solved it", 12 * 60 + 12, 0),
        DemoChat(
            "c8", "a3", "Colombo Logistics", true, "Waybill CL-88231 has been issued", 11 * 60 + 5, 0,
            members = listOf("Ruwan", "Nimali"),
        ),
        DemoChat("c9", "a1", "Ishara Gunawardena", false, "Is the studio available on Friday?", 9 * 60 + 30, 0, yesterday = true),
    )

    // Fully scripted showcase conversation for c1 (mirrors buildConversation).
    private val C1_SCRIPT = listOf(
        DemoMessage("c1-m1", "in", 9 * 60 + 12, "text", "Hi, is the A4 paper carton price still 4,800?"),
        DemoMessage("c1-m2", "out", 9 * 60 + 14, "text", "Yes, it is. Bulk orders above 10 cartons get 5 percent off", "read"),
        DemoMessage("c1-m3", "in", 9 * 60 + 15, "text", "Great. I need 12 cartons delivered to Nugegoda"),
        DemoMessage("c1-m4", "out", 9 * 60 + 20, "media", "Here is the updated invoice", "read"),
        DemoMessage("c1-m5", "in", 9 * 60 + 22, "text", "Perfect, payment will go out today"),
        DemoMessage("c1-m6", "out", 9 * 60 + 23, "text", "Thank you! Delivery will reach you before 6 pm", "read"),
    )

    /** Deterministic conversation script per chat (mirrors buildConversation). */
    fun conversationFor(chat: DemoChat): List<DemoMessage> = when {
        chat.id == "c1" -> C1_SCRIPT
        chat.snippet.startsWith("You: ") -> listOf(
            DemoMessage("${chat.id}-g1", "in", 9 * 60 + 41, "text", "Hey, do you have a quick moment?"),
            DemoMessage("${chat.id}-g2", "out", 9 * 60 + 42, "text", ownBody(chat), "read"),
            DemoMessage("${chat.id}-g3", "in", 9 * 60 + 44, "text", "Yes, that works on my side"),
            DemoMessage("${chat.id}-g4", "out", 9 * 60 + 45, "text", "Great, I will follow up once it is done", "read"),
            DemoMessage("${chat.id}-g5", "in", 9 * 60 + 47, "text", "Thanks, appreciate it"),
        )
        else -> listOf(
            DemoMessage("${chat.id}-g1", "in", 9 * 60 + 41, "text", ownBody(chat)),
            DemoMessage("${chat.id}-g2", "out", 9 * 60 + 43, "text", "Noted, thank you for the update", "read"),
            DemoMessage("${chat.id}-g3", "in", 9 * 60 + 46, "text", "Let me know if anything changes on your side"),
            DemoMessage("${chat.id}-g4", "out", 9 * 60 + 48, "text", "Will do. I will keep you posted", "read"),
        )
    }

    private fun ownBody(chat: DemoChat): String = chat.snippet.removePrefix("You: ")

    /** "a1" + "c1" -> "a1|c1" (composite chat key used everywhere). */
    fun compositeId(accountId: String, chatJid: String): String = "$accountId|$chatJid"

    fun accountById(accountId: String): DemoAccount? = accounts.firstOrNull { it.id == accountId }

    fun chatSeedsFor(accountId: String): List<ChatSeed> =
        chats.filter { it.accountId == accountId }.map { chat ->
            ChatSeed(
                chatId = compositeId(chat.accountId, chat.id),
                name = chat.name,
                isGroup = chat.isGroup,
                snippet = chat.snippet,
                lastTsEpochMs = epochForMinutes(chat.minutesOfDay, chat.yesterday),
                unread = chat.unread,
                members = chat.members,
            )
        }

    /** Seed messages per composite chat id, senders resolved like the web demo. */
    fun conversationsByChat(): Map<String, List<Message>> = chats.associate { chat ->
        var incomingIndex = 0
        compositeId(chat.accountId, chat.id) to conversationFor(chat).map { seed ->
            val incoming = seed.direction == "in"
            val sender = when {
                !incoming -> YOU_LABEL
                chat.isGroup && chat.members.isNotEmpty() ->
                    chat.members[incomingIndex % chat.members.size].also { incomingIndex++ }
                chat.isGroup -> chat.name
                else -> chat.name
            }
            Message(
                id = seed.id,
                chatId = compositeId(chat.accountId, chat.id),
                senderName = sender,
                text = seed.text,
                isFromMe = !incoming,
                timestampEpochMs = epochForMinutes(seed.minutesOfDay, chat.yesterday),
                receipt = when (seed.delivery) {
                    "read" -> Receipt.Read
                    "sent" -> Receipt.Sent
                    else -> null
                },
                kind = if (seed.kind == "media") MessageKind.Media else MessageKind.Text,
            )
        }
    }

    fun epochForMinutes(minutesOfDay: Int, yesterday: Boolean = false): Long {
        val zone = ZoneId.systemDefault()
        var date = LocalDate.now(zone)
        if (yesterday) date = date.minusDays(1)
        return date.atStartOfDay(zone).toInstant().toEpochMilli() + minutesOfDay * 60_000L
    }

    private const val YOU_LABEL = "You"
}
