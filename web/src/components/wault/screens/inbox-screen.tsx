"use client"

import * as React from "react"
import { CheckCheck, Pin, Plus, Search, Users } from "lucide-react"

import { cn } from "@/lib/utils"
import { accountShortName, type Account, type Chat } from "../data"
import {
  Avatar,
  FOCUS_RING,
  HOVER_TINT,
  ScreenTopBar,
  SURFACE_2,
  TEXT_2,
  TEXT_PRIMARY,
  UnreadBadge,
} from "../ui-bits"

export function InboxScreen({
  accounts,
  chats,
  filter,
  onFilter,
  onOpenChat,
  onNewMessage,
  topRight,
}: {
  accounts: Account[]
  chats: Chat[]
  filter: string
  onFilter: (filter: string) => void
  onOpenChat: (chatId: string) => void
  onNewMessage: () => void
  topRight?: React.ReactNode
}) {
  const [query, setQuery] = React.useState("")

  const totalUnread = accounts.reduce((sum, a) => sum + a.unread, 0)
  const visible = chats.filter((chat) => {
    if (filter !== "all" && chat.accountId !== filter) return false
    const q = query.trim().toLowerCase()
    if (!q) return true
    return (
      chat.name.toLowerCase().includes(q) || chat.snippet.toLowerCase().includes(q)
    )
  })

  return (
    <div className="relative flex h-full flex-col">
      <ScreenTopBar title="Inbox" right={topRight} />

      {/* Account filter chips */}
      <div className="no-scrollbar flex shrink-0 gap-2 overflow-x-auto px-4 pb-2">
        <FilterChip active={filter === "all"} onClick={() => onFilter("all")}>
          All · {totalUnread}
        </FilterChip>
        {accounts.map((account) => {
          const count = account.unread
          return (
            <FilterChip
              key={account.id}
              active={filter === account.id}
              accent={account.accent}
              onClick={() => onFilter(account.id)}
            >
              {accountShortName(account.name)}
              {count > 0 ? ` · ${count}` : ""}
            </FilterChip>
          )
        })}
      </div>

      {/* Search */}
      <div className="relative shrink-0 px-4 pb-2">
        <Search
          size={15}
          aria-hidden
          className={cn("pointer-events-none absolute left-7 top-1/2 -translate-y-1/2", TEXT_2)}
        />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search chats"
          aria-label="Search chats"
          className={cn(
            "h-10 w-full rounded-full pl-10 pr-4 text-[13px] outline-none transition-shadow",
            SURFACE_2,
            TEXT_PRIMARY,
            "placeholder:text-[#6B6F76] dark:placeholder:text-[#9AA0A6]",
            "focus:ring-2 focus:ring-[#059669]/50 dark:focus:ring-[#34D399]/50",
          )}
        />
      </div>

      {/* Chat list */}
      <div className="no-scrollbar flex-1 overflow-y-auto px-2 pb-28">
        {visible.length === 0 ? (
          <p className={cn("mt-10 text-center text-[13px]", TEXT_2)}>
            {query.trim()
              ? "No chats match your search"
              : "No chats yet — pair a number and messages land here"}
          </p>
        ) : (
          visible.map((chat) => {
            const account = accounts.find((a) => a.id === chat.accountId)
            return (
              <ChatRow
                key={chat.id}
                chat={chat}
                accent={account?.accent ?? "#10B981"}
                onOpen={() => onOpenChat(chat.id)}
              />
            )
          })
        )}
      </div>

      {/* FAB */}
      <button
        type="button"
        onClick={onNewMessage}
        aria-label="New message"
        className={cn(
          "absolute bottom-[76px] right-4 z-10 grid h-[52px] w-[52px] place-items-center rounded-full text-white shadow-lg transition-transform active:scale-95",
          "bg-[#059669] dark:bg-[#34D399] dark:text-[#0C0D0F]",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669] dark:focus-visible:ring-[#34D399] focus-visible:ring-offset-2 focus-visible:ring-offset-white dark:focus-visible:ring-offset-[#0C0D0F]",
        )}
      >
        <Plus size={22} aria-hidden strokeWidth={2.25} />
      </button>
    </div>
  )
}

function FilterChip({
  children,
  active,
  accent,
  onClick,
}: {
  children: React.ReactNode
  active: boolean
  accent?: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cn(
        "flex h-8 shrink-0 items-center gap-1.5 rounded-full px-3 text-[12px] font-medium transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669] dark:focus-visible:ring-[#34D399]",
        active
          ? "bg-[#17181A] text-[#F7F7F5] dark:bg-[#F2F3F4] dark:text-[#17181A]"
          : cn(SURFACE_2, TEXT_2, "hover:text-[#17181A] dark:hover:text-[#F2F3F4]"),
      )}
    >
      {accent ? (
        <span className="h-2 w-2 rounded-full" style={{ backgroundColor: accent }} aria-hidden />
      ) : null}
      {children}
    </button>
  )
}

function ChatRow({
  chat,
  accent,
  onOpen,
}: {
  chat: Chat
  accent: string
  onOpen: () => void
}) {
  return (
    <button
      type="button"
      onClick={onOpen}
      className={cn(
        "flex w-full items-center gap-3 rounded-2xl px-2 py-2.5 text-left transition-colors",
        HOVER_TINT,
        FOCUS_RING,
      )}
    >
      <Avatar name={chat.name} accent={accent} size={48} ringPx={2} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1">
          <p className={cn("truncate text-[15px] font-medium leading-tight", TEXT_PRIMARY)}>
            {chat.name}
          </p>
          {chat.group ? (
            <Users size={12} aria-hidden className={cn("shrink-0", TEXT_2)} />
          ) : null}
        </div>
        <p className={cn("mt-0.5 truncate text-[13px] leading-snug", TEXT_2)}>{chat.snippet}</p>
        {chat.label ? (
          <span
            className={cn(
              "mt-1 inline-flex max-w-full items-center truncate rounded-full px-2 py-0.5 text-[10px] font-medium",
              SURFACE_2,
              TEXT_2,
            )}
          >
            {chat.label}
          </span>
        ) : null}
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1.5">
        <span className={cn("flex items-center gap-1 text-[11px]", TEXT_2)}>
          {chat.pinned ? <Pin size={10} aria-hidden /> : null}
          {chat.time}
        </span>
        {chat.unread > 0 ? (
          <UnreadBadge count={chat.unread} accent={accent} />
        ) : (
          <CheckCheck size={16} aria-hidden className={TEXT_2} />
        )}
      </div>
    </button>
  )
}
