"use client"

import * as React from "react"
import { AnimatePresence, motion } from "framer-motion"
import {
  CheckCheck,
  Layers,
  Lock,
  MessageCircle,
  Moon,
  Pin,
  Plus,
  Search,
  Settings2,
  ShieldCheck,
  Sun,
  Users,
} from "lucide-react"

import { cn } from "@/lib/utils"
import {
  DEVICE_SLOTS,
  accountShortName,
  type Account,
  type Chat,
  withAlpha,
} from "./data"
import {
  Avatar,
  AvatarCluster,
  BRAND_TEXT,
  FOCUS_RING,
  HAIRLINE,
  HOVER_TINT,
  PHONE_BG,
  PhoneThemeProvider,
  StatusDot,
  SURFACE,
  SURFACE_2,
  TEXT_2,
  TEXT_PRIMARY,
  UnreadBadge,
} from "./ui-bits"
import { AccountSwitcherSheet, NewMessageSheet } from "./account-switcher"
import { liveActions } from "./live"
import { ConversationScreen } from "./screens/conversation-screen"
import { PairingFlow, type PairingResult } from "./screens/pairing-flow"
import { SettingsScreen } from "./screens/settings-screen"
import { VaultScreen } from "./screens/vault-screen"
import { useWaultStore, type WaultTab } from "./wault-app"

/**
 * Web Edition — the same Wault store rendered as a desktop three-pane shell
 * inside a browser-window mock. Rendered instead of WaultApp when the
 * store's view === "web". Like the phone frame, the window carries the
 * `.dark` class so dark mode stays scoped to the shell (page chrome never
 * uses dark: variants).
 */

const FRAME_W = 1100
const FRAME_H = 740

const RAIL_ITEMS: { id: WaultTab; label: string; icon: typeof ShieldCheck }[] = [
  { id: "vault", label: "Vault", icon: ShieldCheck },
  { id: "inbox", label: "Inbox", icon: MessageCircle },
  { id: "settings", label: "Settings", icon: Settings2 },
]

export function WebShell({ variant = "framed" }: { variant?: "framed" | "app" }) {
  const tab = useWaultStore((s) => s.tab)
  const overlay = useWaultStore((s) => s.overlay)
  const sheet = useWaultStore((s) => s.sheet)
  const inboxFilter = useWaultStore((s) => s.inboxFilter)
  const accounts = useWaultStore((s) => s.accounts)
  const chats = useWaultStore((s) => s.chats)
  const theme = useWaultStore((s) => s.theme)
  const mode = useWaultStore((s) => s.mode)
  const live = useWaultStore((s) => s.live)
  const pairing = useWaultStore((s) => s.pairing)
  const messagesByChat = useWaultStore((s) => s.messagesByChat)
  const setTab = useWaultStore((s) => s.setTab)
  const openPairing = useWaultStore((s) => s.openPairing)
  const closeOverlay = useWaultStore((s) => s.closeOverlay)
  const setSheet = useWaultStore((s) => s.setSheet)
  const setInboxFilter = useWaultStore((s) => s.setInboxFilter)
  const addAccount = useWaultStore((s) => s.addAccount)
  const setTheme = useWaultStore((s) => s.setTheme)
  const toggleTheme = useWaultStore((s) => s.toggleTheme)

  const [sheetHost, setSheetHost] = React.useState<HTMLDivElement | null>(null)
  const frameWrapRef = React.useRef<HTMLDivElement>(null)
  const [scale, setScale] = React.useState(1)

  // Fit the 1100px desktop window into narrower viewports (same approach as
  // PhoneFrame). The scale transform also gives vaul's fixed-positioned
  // sheets a containing block, keeping them inside the window frame.
  React.useEffect(() => {
    if (variant === "app") return
    const el = frameWrapRef.current
    if (!el) return
    const measure = () => {
      const w = el.clientWidth
      setScale(w > 0 ? Math.min(1, w / FRAME_W) : 1)
    }
    measure()
    const observer = new ResizeObserver(measure)
    observer.observe(el)
    return () => observer.disconnect()
  }, [variant])

  const dark = theme === "dark"
  const slotsFull = accounts.length >= DEVICE_SLOTS
  const totalUnread = React.useMemo(
    () => accounts.reduce((sum, a) => sum + a.unread, 0),
    [accounts],
  )

  const completePairing = (result: PairingResult) => {
    if (mode === "app" && live) {
      // Live mode: the account was created by the bridge at pairing start.
      closeOverlay()
      setTab("vault")
      return
    }
    addAccount({
      id: `a${accounts.length + 1}`,
      name: result.name,
      phone: "+94 77 042 8810",
      accent: result.accent,
      status: "connected",
      unread: 0,
      receivedToday: 0,
      sentToday: 0,
      lastActive: "just now",
    })
    closeOverlay()
    setTab("vault")
  }

  const openWorkspace = (accountId: string) => {
    setInboxFilter(accountId)
    setTab("inbox")
  }

  const topRight = <AvatarCluster accounts={accounts} onOpen={() => setSheet("switcher")} />

  // Detail-pane conversation — web keeps it inline instead of full-screen.
  let conversationNode: React.ReactNode = null
  if (overlay?.type === "conversation") {
    const chat = chats.find((c) => c.id === overlay.chatId)
    const account = chat ? accounts.find((a) => a.id === chat.accountId) : undefined
    if (chat && account) {
      conversationNode = (
        <ConversationScreen
          key={chat.id}
          chat={chat}
          account={account}
          onBack={closeOverlay}
          live={
            mode === "app" && live
              ? {
                  messages: messagesByChat[chat.id] ?? [],
                  onSend: (text: string) => {
                    void liveActions.sendMessage(chat.id, text)
                  },
                }
              : undefined
          }
        />
      )
    }
  }
  const activeChatId = overlay?.type === "conversation" ? overlay.chatId : null
  // Defensive: a conversation opened elsewhere (e.g. via the quick-jump chip
  // while the phone view was active) still surfaces in the inbox context.
  const showInbox = tab === "inbox" || overlay?.type === "conversation"

  const cssVars = {
    "--wa-bg": dark ? "#0C0D0F" : "#F7F7F5",
    "--wa-hairline": dark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.07)",
  } as React.CSSProperties

  const frameInner = (
    <>
      <PhoneThemeProvider dark={dark}>
              <div className="relative flex min-h-0 flex-1">
                {/* Region 1 — icon rail */}
                <nav
                  aria-label="Primary"
                  className={cn(
                    "flex w-16 shrink-0 flex-col items-center border-r py-3",
                    SURFACE,
                    HAIRLINE,
                  )}
                >
                  <span
                    aria-hidden
                    className="grid h-9 w-9 shrink-0 place-items-center rounded-xl"
                    style={{ backgroundColor: withAlpha("#10B981", dark ? 0.16 : 0.12) }}
                  >
                    <ShieldCheck size={20} className={BRAND_TEXT} />
                  </span>

                  <div className="mt-6 flex flex-col items-center gap-2">
                    {RAIL_ITEMS.map((item) => {
                      const active = tab === item.id
                      const Icon = item.icon
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => setTab(item.id)}
                          aria-label={item.label}
                          aria-current={active ? "page" : undefined}
                          className={cn(
                            "flex w-14 flex-col items-center gap-1 rounded-2xl py-0.5 focus-visible:outline-none",
                            FOCUS_RING,
                          )}
                        >
                          <span
                            className={cn(
                              "relative grid h-11 w-11 place-items-center rounded-full transition-colors duration-200",
                              active ? SURFACE_2 : "bg-transparent",
                            )}
                          >
                            <Icon
                              size={20}
                              aria-hidden
                              strokeWidth={active ? 2.25 : 2}
                              className={cn(
                                "transition-colors duration-200",
                                active ? TEXT_PRIMARY : TEXT_2,
                              )}
                            />
                            {item.id === "inbox" && totalUnread > 0 ? (
                              <span className="absolute -right-0.5 -top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-[#059669] px-1 text-[9px] font-bold text-white dark:bg-[#34D399] dark:text-[#0C0D0F]">
                                {totalUnread}
                              </span>
                            ) : null}
                          </span>
                          <span
                            className={cn(
                              "text-[9px] leading-none",
                              active ? cn("font-medium", TEXT_PRIMARY) : TEXT_2,
                            )}
                          >
                            {item.label}
                          </span>
                        </button>
                      )
                    })}
                  </div>

                  <div className="mt-auto flex flex-col items-center gap-2">
                    <button
                      type="button"
                      onClick={toggleTheme}
                      aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
                      className={cn(
                        "grid h-11 w-11 place-items-center rounded-full transition-colors",
                        HOVER_TINT,
                        FOCUS_RING,
                        TEXT_2,
                      )}
                    >
                      {dark ? <Sun size={18} aria-hidden /> : <Moon size={18} aria-hidden />}
                    </button>
                    <AvatarCluster accounts={accounts} onOpen={() => setSheet("switcher")} />
                  </div>
                </nav>

                {/* Region 2 — workspace rail */}
                <aside
                  aria-label="Workspaces"
                  className={cn("flex w-[232px] shrink-0 flex-col border-r", PHONE_BG, HAIRLINE)}
                >
                  <p className={cn("px-4 pt-4 text-[12px] font-medium uppercase tracking-wide", TEXT_2)}>
                    Workspaces
                  </p>

                  <div className="flex flex-col gap-1 px-3 pt-3">
                    <button
                      type="button"
                      onClick={() => openWorkspace("all")}
                      aria-pressed={inboxFilter === "all"}
                      className={cn(
                        "flex h-12 w-full items-center gap-3 rounded-lg px-2 text-left transition-colors",
                        FOCUS_RING,
                        inboxFilter === "all" ? SURFACE_2 : HOVER_TINT,
                      )}
                    >
                      <span className={cn("grid h-8 w-8 shrink-0 place-items-center rounded-lg", SURFACE_2)}>
                        <Layers size={16} aria-hidden className={TEXT_PRIMARY} />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span
                          className={cn("block truncate text-[13px] font-medium leading-tight", TEXT_PRIMARY)}
                        >
                          All workspaces
                        </span>
                        <span className={cn("mt-0.5 block truncate text-[11px] leading-tight", TEXT_2)}>
                          Every chat
                        </span>
                      </span>
                    </button>

                    {accounts.map((account) => {
                      const active = inboxFilter === account.id
                      return (
                        <button
                          key={account.id}
                          type="button"
                          onClick={() => openWorkspace(account.id)}
                          aria-pressed={active}
                          aria-label={`Show chats for ${account.name}`}
                          className={cn(
                            "flex h-12 w-full items-center gap-3 rounded-lg px-2 text-left transition-colors",
                            FOCUS_RING,
                            active ? SURFACE_2 : HOVER_TINT,
                          )}
                        >
                          <Avatar name={account.name} accent={account.accent} size={32} ringPx={1.5} />
                          <span
                            className={cn("min-w-0 flex-1 truncate text-[13px] font-medium", TEXT_PRIMARY)}
                          >
                            {account.name}
                          </span>
                          <StatusDot account={account} />
                          <UnreadBadge count={account.unread} accent={account.accent} />
                        </button>
                      )
                    })}

                    <button
                      type="button"
                      onClick={openPairing}
                      disabled={slotsFull}
                      aria-label="Pair new account"
                      className={cn(
                        "flex h-12 w-full items-center gap-3 rounded-lg px-2 text-left transition-colors",
                        FOCUS_RING,
                        slotsFull ? "cursor-not-allowed" : HOVER_TINT,
                      )}
                    >
                      <span
                        className={cn(
                          "grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-dashed",
                          SURFACE_2,
                          slotsFull
                            ? "border-black/15 dark:border-white/15"
                            : "border-black/25 dark:border-white/30",
                        )}
                      >
                        <Plus size={16} aria-hidden className={slotsFull ? TEXT_2 : TEXT_PRIMARY} />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span
                          className={cn(
                            "block truncate text-[13px] font-medium leading-tight",
                            slotsFull ? TEXT_2 : TEXT_PRIMARY,
                          )}
                        >
                          Pair new account
                        </span>
                        <span className={cn("mt-0.5 block truncate text-[11px] leading-tight", TEXT_2)}>
                          {slotsFull ? "Device slots full" : "Links as a companion device"}
                        </span>
                      </span>
                    </button>
                  </div>

                  {/* Mini session card */}
                  <div className="mt-auto p-3">
                    <div className={cn("rounded-2xl p-3", SURFACE)}>
                      <div className="flex items-center justify-between">
                        <span className={cn("text-[11px]", TEXT_2)}>Device slots</span>
                        <span className={cn("text-[11px] font-semibold", TEXT_PRIMARY)}>
                          {accounts.length} of {DEVICE_SLOTS}
                        </span>
                      </div>
                      <div className={cn("mt-2 h-1 overflow-hidden rounded-full", SURFACE_2)}>
                        <div
                          className="h-full rounded-full bg-[#059669] transition-[width] duration-300 dark:bg-[#34D399]"
                          style={{ width: `${(accounts.length / DEVICE_SLOTS) * 100}%` }}
                        />
                      </div>
                      <p className={cn("mt-2 flex items-center gap-1.5 text-[10px]", TEXT_2)}>
                        <Lock size={11} aria-hidden />
                        Keys on bridge vault
                      </p>
                    </div>
                  </div>
                </aside>

                {/* Region 3 — content */}
                <section className="relative flex min-w-0 flex-1">
                  {showInbox ? (
                    <InboxPane
                      accounts={accounts}
                      chats={chats}
                      filter={inboxFilter}
                      activeChatId={activeChatId}
                      conversationNode={conversationNode}
                      onFilter={setInboxFilter}
                      onOpenChat={(chatId) => liveActions.openChat(chatId)}
                      onNewMessage={() => setSheet("newmessage")}
                    />
                  ) : null}

                  {tab === "vault" && !showInbox ? (
                    <div className="no-scrollbar h-full w-full overflow-y-auto">
                      <div className="mx-auto w-full max-w-[760px] p-6">
                        <VaultScreen
                          accounts={accounts}
                          topRight={topRight}
                          onOpenAccount={openWorkspace}
                          onPair={openPairing}
                        />
                      </div>
                    </div>
                  ) : null}

                  {tab === "settings" && !showInbox ? (
                    <div className="no-scrollbar h-full w-full overflow-y-auto">
                      <div className="mx-auto w-full max-w-[640px] p-6">
                        <SettingsScreen
                          accounts={accounts}
                          theme={theme}
                          onTheme={setTheme}
                          onOpenChannels={() => setSheet("channels")}
                          sheetHost={sheetHost}
                          channelsOpen={sheet === "channels"}
                          onCloseChannels={() => setSheet(null)}
                          topRight={topRight}
                        />
                      </div>
                    </div>
                  ) : null}
                </section>

                {/* Pairing — centered modal over the whole app area */}
                <AnimatePresence>
                  {overlay?.type === "pairing" ? (
                    <motion.div
                      key="web-pairing"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.2, ease: "easeOut" }}
                      className="absolute inset-0 z-40 grid place-items-center bg-black/30 p-6 backdrop-blur-[2px] dark:bg-black/50"
                      onClick={closeOverlay}
                    >
                      <div
                        role="dialog"
                        aria-modal="true"
                        aria-label="Pair new account"
                        onClick={(e) => e.stopPropagation()}
                        className={cn(
                          "relative h-[600px] max-h-[90%] w-[420px] max-w-[92%] overflow-hidden rounded-3xl shadow-2xl",
                          SURFACE,
                        )}
                      >
                        <PairingFlow
                          onClose={closeOverlay}
                          onComplete={completePairing}
                          live={
                            mode === "app"
                              ? {
                                  qrDataUrl: pairing?.qrDataUrl ?? null,
                                  linked: pairing?.linked ?? false,
                                  onStart: (name: string, accentHex: string) => {
                                    void liveActions.startPairing(name, accentHex)
                                  },
                                }
                              : undefined
                          }
                        />
                      </div>
                    </motion.div>
                  ) : null}
                </AnimatePresence>
              </div>
      </PhoneThemeProvider>

      {/* Portal host for vaul sheets; the scaled ancestor scopes their
          fixed positioning to this window frame. */}
      <div ref={setSheetHost} className="contents" />
    </>
  )

  const sheets = (
    <>
      {/* Shared sheets — portaled into the shell */}
      <AccountSwitcherSheet
        open={sheet === "switcher"}
        onOpenChange={(open) => {
          if (!open) setSheet(null)
        }}
        host={sheetHost}
        accounts={accounts}
        onPickAll={() => {
          setInboxFilter("all")
          setTab("inbox")
          setSheet(null)
        }}
        onPick={(accountId) => {
          setInboxFilter(accountId)
          setTab("inbox")
          setSheet(null)
        }}
        onPair={() => {
          setSheet(null)
          openPairing()
        }}
      />
      <NewMessageSheet
        open={sheet === "newmessage"}
        onOpenChange={(open) => {
          if (!open) setSheet(null)
        }}
        host={sheetHost}
        accounts={accounts}
      />
    </>
  )

  if (variant === "app") {
    return (
      <>
        <div
          className={cn(
            "relative flex h-full w-full flex-col overflow-hidden bg-[#F7F7F5] dark:bg-[#0C0D0F]",
            dark && "dark",
          )}
          style={cssVars}
        >
          {frameInner}
        </div>
        {sheets}
      </>
    )
  }

  return (
    <div className="my-10">
      <div ref={frameWrapRef} className="relative w-full" style={{ height: FRAME_H * scale }}>
        <div
          className="mx-auto w-[1100px] shrink-0"
          style={{ transform: `scale(${scale})`, transformOrigin: "top center", height: FRAME_H }}
        >
          <div
            className={cn(
              "relative flex h-[740px] w-full flex-col overflow-hidden rounded-2xl border",
              "border-black/[0.08] bg-[#F7F7F5] shadow-[0_40px_80px_-24px_rgba(0,0,0,0.3)]",
              "dark:border-white/[0.1] dark:bg-[#0C0D0F]",
              dark && "dark",
            )}
            style={cssVars}
          >
            <ChromeBar />
            {frameInner}
          </div>
        </div>
      </div>
      {sheets}
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* Browser chrome bar                                                  */
/* ------------------------------------------------------------------ */

function ChromeBar() {
  return (
    <div
      className={cn(
        "relative flex h-11 shrink-0 items-center justify-between border-b px-4",
        SURFACE,
        HAIRLINE,
      )}
    >
      <span aria-hidden className="flex shrink-0 items-center gap-1.5">
        <span className="h-2.5 w-2.5 rounded-full bg-black/[0.14] dark:bg-white/[0.16]" />
        <span className="h-2.5 w-2.5 rounded-full bg-black/[0.14] dark:bg-white/[0.16]" />
        <span className="h-2.5 w-2.5 rounded-full bg-black/[0.14] dark:bg-white/[0.16]" />
      </span>

      <span
        className={cn(
          "absolute left-1/2 top-1/2 flex max-w-[55%] -translate-x-1/2 -translate-y-1/2 items-center gap-1.5 rounded-full px-3 py-1 text-[12px]",
          SURFACE_2,
          TEXT_2,
        )}
      >
        <Lock size={12} aria-hidden className="shrink-0" />
        <span className="truncate">wault-office.local</span>
      </span>

      <span
        className={cn(
          "flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px]",
          SURFACE_2,
          TEXT_2,
        )}
      >
        <motion.span
          className="h-2 w-2 rounded-full"
          style={{ backgroundColor: "#10B981" }}
          animate={{ opacity: [1, 0.4, 1] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
          aria-hidden
        />
        Bridge · connected
      </span>
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* Inbox — two panes: chat list + conversation detail                  */
/* ------------------------------------------------------------------ */

function InboxPane({
  accounts,
  chats,
  filter,
  activeChatId,
  conversationNode,
  onFilter,
  onOpenChat,
  onNewMessage,
}: {
  accounts: Account[]
  chats: Chat[]
  filter: string
  activeChatId: string | null
  conversationNode: React.ReactNode
  onFilter: (filter: string) => void
  onOpenChat: (chatId: string) => void
  onNewMessage: () => void
}) {
  const [query, setQuery] = React.useState("")
  const totalUnread = accounts.reduce((sum, a) => sum + a.unread, 0)

  const visible = chats.filter((chat) => {
    if (filter !== "all" && chat.accountId !== filter) return false
    const q = query.trim().toLowerCase()
    if (!q) return true
    return chat.name.toLowerCase().includes(q) || chat.snippet.toLowerCase().includes(q)
  })

  return (
    <div className="flex min-h-0 flex-1">
      {/* List pane */}
      <div className={cn("flex w-[340px] shrink-0 flex-col border-r", PHONE_BG, HAIRLINE)}>
        <div className="flex items-center gap-2 px-3 pb-2 pt-3">
          <div className="relative min-w-0 flex-1">
            <Search
              size={15}
              aria-hidden
              className={cn("pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2", TEXT_2)}
            />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search chats"
              aria-label="Search chats"
              className={cn(
                "h-10 w-full rounded-full pl-9 pr-4 text-[13px] outline-none transition-shadow",
                SURFACE_2,
                TEXT_PRIMARY,
                "placeholder:text-[#6B6F76] dark:placeholder:text-[#9AA0A6]",
                "focus:ring-2 focus:ring-[#059669]/50 dark:focus:ring-[#34D399]/50",
              )}
            />
          </div>
          <button
            type="button"
            onClick={onNewMessage}
            aria-label="New message"
            className={cn(
              "grid h-10 w-10 shrink-0 place-items-center rounded-full transition-colors",
              SURFACE_2,
              TEXT_PRIMARY,
              "hover:bg-black/[0.06] dark:hover:bg-white/[0.1]",
              FOCUS_RING,
            )}
          >
            <Plus size={18} aria-hidden />
          </button>
        </div>

        {filter === "all" ? (
          <div className="flex flex-wrap gap-2 px-3 pb-2">
            <WebFilterChip active onClick={() => onFilter("all")}>
              All · {totalUnread}
            </WebFilterChip>
            {accounts.map((account) => (
              <WebFilterChip
                key={account.id}
                active={false}
                accent={account.accent}
                onClick={() => onFilter(account.id)}
              >
                {accountShortName(account.name)}
                {account.unread > 0 ? ` · ${account.unread}` : ""}
              </WebFilterChip>
            ))}
          </div>
        ) : null}

        <div className="no-scrollbar flex-1 overflow-y-auto px-2 pb-3">
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
                <WebChatRow
                  key={chat.id}
                  chat={chat}
                  accent={account?.accent ?? "#10B981"}
                  selected={activeChatId === chat.id}
                  onOpen={() => onOpenChat(chat.id)}
                />
              )
            })
          )}
        </div>
      </div>

      {/* Detail pane */}
      <div className="relative min-w-0 flex-1 overflow-hidden">
        <AnimatePresence initial={false}>{conversationNode}</AnimatePresence>
        {conversationNode === null ? <DetailEmptyState /> : null}
      </div>
    </div>
  )
}

function DetailEmptyState() {
  return (
    <div className="flex h-full flex-col items-center justify-center px-6 text-center">
      <span aria-hidden className={cn("grid h-[72px] w-[72px] place-items-center rounded-full", SURFACE_2)}>
        <ShieldCheck size={40} className={BRAND_TEXT} />
      </span>
      <p className={cn("mt-3 text-[15px] font-medium", TEXT_PRIMARY)}>Select a conversation</p>
      <p className={cn("mt-1 text-[12px]", TEXT_2)}>Messages sync from your office bridge</p>
    </div>
  )
}

function WebFilterChip({
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
        <span aria-hidden className="h-2 w-2 rounded-full" style={{ backgroundColor: accent }} />
      ) : null}
      {children}
    </button>
  )
}

function WebChatRow({
  chat,
  accent,
  selected,
  onOpen,
}: {
  chat: Chat
  accent: string
  selected: boolean
  onOpen: () => void
}) {
  return (
    <button
      type="button"
      onClick={onOpen}
      aria-current={selected ? "true" : undefined}
      className={cn(
        "flex w-full items-center gap-3 rounded-2xl px-2 py-2.5 text-left transition-colors",
        selected ? SURFACE_2 : HOVER_TINT,
        FOCUS_RING,
      )}
    >
      <Avatar name={chat.name} accent={accent} size={40} ringPx={1.5} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1">
          <p className={cn("truncate text-[14px] font-medium leading-tight", TEXT_PRIMARY)}>
            {chat.name}
          </p>
          {chat.group ? <Users size={12} aria-hidden className={cn("shrink-0", TEXT_2)} /> : null}
        </div>
        <p className={cn("mt-0.5 truncate text-[12px] leading-snug", TEXT_2)}>{chat.snippet}</p>
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
