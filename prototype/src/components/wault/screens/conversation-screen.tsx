"use client"

import * as React from "react"
import { motion } from "framer-motion"
import {
  ArrowLeft,
  Check,
  CheckCheck,
  FileText,
  Mic,
  Phone,
  Plus,
  Send,
  Video,
} from "lucide-react"

import { cn } from "@/lib/utils"
import {
  buildConversation,
  QUICK_REPLIES,
  withAlpha,
  type Account,
  type Chat,
  type Message,
} from "../data"
import {
  Avatar,
  FOCUS_RING,
  HAIRLINE,
  PHONE_BG,
  SURFACE,
  SURFACE_2,
  TEXT_2,
  TEXT_PRIMARY,
  usePhoneDark,
} from "../ui-bits"

export function ConversationScreen({
  chat,
  account,
  onBack,
}: {
  chat: Chat
  account: Account
  onBack: () => void
}) {
  const dark = usePhoneDark()
  const accent = account.accent
  const [messages, setMessages] = React.useState<Message[]>(() => buildConversation(chat))
  const [draft, setDraft] = React.useState("")
  const listRef = React.useRef<HTMLDivElement>(null)
  const timersRef = React.useRef<number[]>([])

  const scrollToBottom = React.useCallback(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [])

  React.useEffect(() => {
    scrollToBottom()
  }, [scrollToBottom])

  React.useEffect(() => {
    const timers = timersRef.current
    return () => timers.forEach((t) => window.clearTimeout(t))
  }, [])

  React.useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  const send = () => {
    const text = draft.trim()
    if (!text) return
    const now = new Date()
    const time = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`
    const id = `sent-${messages.length + 1}`
    setMessages((prev) => [
      ...prev,
      { id, direction: "out", time, kind: "text", text, delivery: "sent" },
    ])
    setDraft("")
    const timer = window.setTimeout(() => {
      setMessages((prev) =>
        prev.map((m) => (m.id === id ? { ...m, delivery: "read" as const } : m)),
      )
    }, 1500)
    timersRef.current.push(timer)
  }

  return (
    <motion.div
      initial={{ x: "100%" }}
      animate={{ x: 0 }}
      exit={{ x: "100%" }}
      transition={{ type: "spring", damping: 30, stiffness: 300 }}
      className={cn("absolute inset-0 z-30 flex flex-col", PHONE_BG)}
    >
      {/* Header */}
      <header
        className={cn("flex h-14 shrink-0 items-center gap-2 border-b px-2", SURFACE, HAIRLINE)}
      >
        <button
          type="button"
          onClick={onBack}
          aria-label="Back"
          className={cn(
            "grid h-10 w-10 shrink-0 place-items-center rounded-full transition-colors",
            "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
            FOCUS_RING,
            TEXT_PRIMARY,
          )}
        >
          <ArrowLeft size={20} aria-hidden />
        </button>
        <Avatar name={chat.name} accent={accent} size={36} ringPx={1.5} />
        <div className="min-w-0 flex-1">
          <p className={cn("truncate text-[15px] font-medium leading-tight", TEXT_PRIMARY)}>
            {chat.name}
          </p>
          <p className={cn("text-[11px] leading-tight", TEXT_2)}>
            {chat.id === "c1" ? "online" : "last seen recently"}
          </p>
        </div>
        <button
          type="button"
          aria-label="Voice call"
          className={cn(
            "grid h-10 w-10 shrink-0 place-items-center rounded-full transition-colors",
            "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
            FOCUS_RING,
            TEXT_2,
          )}
        >
          <Phone size={20} aria-hidden />
        </button>
        <button
          type="button"
          aria-label="Video call"
          className={cn(
            "grid h-10 w-10 shrink-0 place-items-center rounded-full transition-colors",
            "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
            FOCUS_RING,
            TEXT_2,
          )}
        >
          <Video size={20} aria-hidden />
        </button>
      </header>

      {/* Account chip strip */}
      <div
        className="flex h-8 shrink-0 items-center gap-2 px-4"
        style={{ backgroundColor: withAlpha(accent, dark ? 0.12 : 0.08) }}
      >
        <span className="h-2 w-2 rounded-full" style={{ backgroundColor: accent }} aria-hidden />
        <span className={cn("text-[11px] font-medium", TEXT_PRIMARY)}>{account.name}</span>
        <span className={cn("text-[11px]", TEXT_2)}>via Wault</span>
      </div>

      {/* Messages */}
      <div ref={listRef} className="no-scrollbar flex flex-1 flex-col gap-2.5 overflow-y-auto px-4 py-3">
        <div className="flex justify-center pb-1">
          <span className={cn("rounded-full px-2.5 py-1 text-[11px] font-medium", SURFACE_2, TEXT_2)}>
            Today
          </span>
        </div>
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} accent={accent} />
        ))}
      </div>

      {/* Composer */}
      <div className="shrink-0 px-3 pb-2">
        <div className="no-scrollbar flex gap-2 overflow-x-auto pb-2">
          {QUICK_REPLIES.map((reply) => (
            <button
              key={reply}
              type="button"
              onClick={() => setDraft(reply)}
              className={cn(
                "shrink-0 rounded-full px-3 py-1.5 text-[12px] font-medium transition-colors",
                SURFACE_2,
                TEXT_PRIMARY,
                "hover:bg-black/[0.06] dark:hover:bg-white/[0.1]",
                FOCUS_RING,
              )}
            >
              {reply}
            </button>
          ))}
        </div>
        <div className={cn("flex h-11 items-center gap-1 rounded-full pl-2 pr-1.5", SURFACE_2)}>
          <button
            type="button"
            aria-label="Attach"
            className={cn(
              "grid h-8 w-8 shrink-0 place-items-center rounded-full transition-colors",
              "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
              FOCUS_RING,
              TEXT_2,
            )}
          >
            <Plus size={18} aria-hidden />
          </button>
          <input
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") send()
            }}
            placeholder="Message"
            aria-label="Message"
            className={cn(
              "h-full min-w-0 flex-1 bg-transparent text-[14px] outline-none",
              TEXT_PRIMARY,
              "placeholder:text-[#6B6F76] dark:placeholder:text-[#9AA0A6]",
            )}
          />
          {draft.trim() ? (
            <button
              type="button"
              onClick={send}
              aria-label="Send message"
              className={cn(
                "grid h-9 w-9 shrink-0 place-items-center rounded-full text-white transition-transform active:scale-95",
                "bg-[#059669] dark:bg-[#34D399] dark:text-[#0C0D0F]",
                FOCUS_RING,
              )}
            >
              <Send size={16} aria-hidden />
            </button>
          ) : (
            <button
              type="button"
              aria-label="Record voice message"
              className={cn(
                "grid h-9 w-9 shrink-0 place-items-center rounded-full transition-colors",
                "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
                FOCUS_RING,
                TEXT_2,
              )}
            >
              <Mic size={18} aria-hidden />
            </button>
          )}
        </div>
      </div>
    </motion.div>
  )
}

function MessageBubble({ message, accent }: { message: Message; accent: string }) {
  const dark = usePhoneDark()
  const incoming = message.direction === "in"

  const bubbleStyle: React.CSSProperties = incoming
    ? {}
    : { backgroundColor: withAlpha(accent, dark ? 0.22 : 0.14) }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className={cn("flex max-w-[78%]", incoming ? "self-start" : "ml-auto self-end")}
    >
      <div
        className={cn(
          "min-w-0 rounded-2xl p-3",
          incoming ? "rounded-bl-md bg-white dark:bg-[#1E2126]" : "rounded-br-md",
        )}
        style={bubbleStyle}
      >
        {message.kind === "media" ? (
          <div className="mb-1.5">
            <div className="grid h-40 w-52 place-items-center rounded-xl bg-gradient-to-br from-emerald-200 to-emerald-400">
              <FileText size={28} aria-hidden className="text-white" />
            </div>
          </div>
        ) : null}
        {message.kind === "media" && message.caption ? (
          <p className={cn("text-[14px] leading-snug", TEXT_PRIMARY)}>{message.caption}</p>
        ) : null}
        {message.kind === "text" && message.text ? (
          <p className={cn("text-[14px] leading-snug", TEXT_PRIMARY)}>{message.text}</p>
        ) : null}
        <span
          className={cn("mt-1 flex items-center justify-end gap-1 text-[10px]", TEXT_2)}
        >
          {message.time}
          {!incoming ? (
            message.delivery === "read" ? (
              <CheckCheck size={14} aria-hidden style={{ color: accent }} />
            ) : (
              <Check size={14} aria-hidden />
            )
          ) : null}
        </span>
      </div>
    </motion.div>
  )
}
