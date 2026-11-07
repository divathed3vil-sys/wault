"use client"

import * as React from "react"
import { AnimatePresence, motion } from "framer-motion"
import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import { cn } from "@/lib/utils"
import {
  INITIAL_ACCOUNTS,
  INITIAL_CHATS,
  type Account,
  type Chat,
  type Message,
} from "./data"
import { liveActions } from "./live"
import { PhoneFrame } from "./phone-frame"
import { BottomNav } from "./bottom-nav"
import { AvatarCluster, PhoneThemeProvider } from "./ui-bits"
import { AccountSwitcherSheet, NewMessageSheet } from "./account-switcher"
import { VaultScreen } from "./screens/vault-screen"
import { InboxScreen } from "./screens/inbox-screen"
import { ConversationScreen } from "./screens/conversation-screen"
import { PairingFlow, type PairingResult } from "./screens/pairing-flow"
import { SettingsScreen } from "./screens/settings-screen"

/* ------------------------------------------------------------------ */
/* State model                                                         */
/* ------------------------------------------------------------------ */

export type WaultTab = "vault" | "inbox" | "settings"

export type WaultOverlay =
  | { type: "conversation"; chatId: string }
  | { type: "pairing" }
  | null

export type WaultSheetKind = "switcher" | "newmessage" | "channels" | null

export type ThemePref = "light" | "dark" | "system"

/** Which shell renders on the page: the Android phone or the web edition. */
export type WaultView = "phone" | "web"

/** Page-level mode: the actual app (bridge-backed) or the design showcase. */
export type WaultPageMode = "app" | "prototype"

export interface WaultPairingState {
  accountId: string
  qrDataUrl: string | null
  linked: boolean
}

interface WaultStore {
  tab: WaultTab
  overlay: WaultOverlay
  sheet: WaultSheetKind
  inboxFilter: string
  accounts: Account[]
  chats: Chat[]
  theme: ThemePref
  /** "phone" renders the Android shell, "web" the desktop web edition. */
  view: WaultView
  /** "app" = live bridge-backed app, "prototype" = design showcase. */
  mode: WaultPageMode
  /** True while the Wault Bridge socket is connected. */
  live: boolean
  /** Active pairing session (live mode only). */
  pairing: WaultPairingState | null
  /** Live conversation messages, keyed by `${accountId}|${chatJid}`. */
  messagesByChat: Record<string, Message[]>
  /** Flips true once persisted state has been merged (client only). */
  hydrated: boolean

  setTab: (tab: WaultTab) => void
  openConversation: (chatId: string) => void
  openPairing: () => void
  closeOverlay: () => void
  setSheet: (sheet: WaultSheetKind) => void
  setInboxFilter: (filter: string) => void
  addAccount: (account: Account) => void
  setTheme: (theme: ThemePref) => void
  toggleTheme: () => void
  setView: (view: WaultView) => void
  setMode: (mode: WaultPageMode) => void
  setLive: (live: boolean) => void
  setPairing: (pairing: WaultPairingState | null) => void
  seedLiveAccounts: (accounts: Account[]) => void
  applyAccounts: (accounts: Account[]) => void
  setLiveChats: (
    chats: Chat[],
    stats: Record<string, { receivedToday: number; sentToday: number }>,
  ) => void
  appendMessage: (chatKey: string, message: Message) => void
  addMessages: (chatKey: string, messages: Message[]) => void
  replaceMessage: (chatKey: string, tmpId: string, message: Message) => void
  applyLiveMessage: (
    accountId: string,
    chatJid: string,
    message: Message,
    key: string,
  ) => void
  applyMessageStatus: (chatKey: string, messageId: string, status: number) => void
  setHydrated: () => void
  reset: () => void
  resetToDemo: () => void
}

/** Fields written to localStorage — everything else stays ephemeral. */
type PersistedWaultState = Pick<WaultStore, "theme" | "inboxFilter" | "view" | "mode">

export const useWaultStore = create<WaultStore>()(
  persist(
    (set, get) => ({
      tab: "vault",
      overlay: null,
      sheet: null,
      inboxFilter: "all",
      accounts: INITIAL_ACCOUNTS,
      chats: INITIAL_CHATS,
      theme: "light",
      view: "phone",
      mode: "app",
      live: false,
      pairing: null,
      messagesByChat: {},
      hydrated: false,

      setTab: (tab) => set({ tab, overlay: null }),

      openConversation: (chatId) =>
        set((state) => {
          const chat = state.chats.find((c) => c.id === chatId)
          if (!chat) return { overlay: { type: "conversation", chatId } as WaultOverlay }
          const patch: Partial<WaultStore> = {
            overlay: { type: "conversation", chatId } as WaultOverlay,
            chats: state.chats.map((c) => (c.id === chatId ? { ...c, unread: 0 } : c)),
            accounts: state.accounts.map((a) =>
              a.id === chat.accountId
                ? { ...a, unread: Math.max(0, a.unread - chat.unread) }
                : a,
            ),
          }
          if (state.view === "web") {
            // Web edition: conversations open in the inbox detail pane, so
            // make sure the inbox tab is up and align the workspace filter
            // with the opened chat when a foreign filter was active.
            patch.tab = "inbox"
            if (state.inboxFilter !== "all" && state.inboxFilter !== chat.accountId) {
              patch.inboxFilter = chat.accountId
            }
          }
          return patch
        }),

      openPairing: () => set({ overlay: { type: "pairing" } }),
      closeOverlay: () => set({ overlay: null }),
      setSheet: (sheet) => set({ sheet }),
      setInboxFilter: (inboxFilter) => set({ inboxFilter }),
      addAccount: (account) => set((state) => ({ accounts: [...state.accounts, account] })),
      setTheme: (theme) => set({ theme }),
      toggleTheme: () => set((state) => ({ theme: state.theme === "dark" ? "light" : "dark" })),
      setView: (view) => set({ view }),
      setMode: (mode) => set({ mode }),
      setLive: (live) => set({ live }),
      setPairing: (pairing) => set({ pairing }),

      seedLiveAccounts: (incoming) =>
        set((state) => {
          // Only takes over while the store still holds the demo dataset.
          const isDemo = state.accounts.some((a) => a.id === "a1")
          if (!isDemo && state.accounts.length > 0) return {}
          return { accounts: incoming, chats: [], messagesByChat: {} }
        }),

      applyAccounts: (incoming) =>
        set((state) => {
          const byId = new Map(state.accounts.map((a) => [a.id, a]))
          const accounts = incoming.map((a) => {
            const prev = byId.get(a.id)
            return prev
              ? {
                  ...a,
                  unread: prev.unread,
                  receivedToday: prev.receivedToday,
                  sentToday: prev.sentToday,
                  lastActive: prev.lastActive,
                }
              : a
          })
          return { accounts }
        }),

      setLiveChats: (chats, stats) =>
        set((state) => {
          const unreadByAccount = new Map<string, number>()
          for (const c of chats) {
            unreadByAccount.set(
              c.accountId,
              (unreadByAccount.get(c.accountId) ?? 0) + c.unread,
            )
          }
          const accounts = state.accounts.map((a) => {
            const s = stats[a.id]
            if (!s && !unreadByAccount.has(a.id)) return a
            return {
              ...a,
              unread: unreadByAccount.get(a.id) ?? 0,
              receivedToday: s?.receivedToday ?? a.receivedToday,
              sentToday: s?.sentToday ?? a.sentToday,
              lastActive: s ? "live" : a.lastActive,
            }
          })
          return { chats, accounts }
        }),

      appendMessage: (chatKey, message) =>
        set((state) => {
          const list = state.messagesByChat[chatKey] ?? []
          if (list.some((m) => m.id === message.id)) return {}
          const chat = state.chats.find((c) => c.id === chatKey)
          const isOpen =
            state.overlay?.type === "conversation" && state.overlay.chatId === chatKey
          let chats = state.chats
          let accounts = state.accounts
          if (chat && message.direction === "in" && !isOpen) {
            chats = state.chats.map((c) =>
              c.id === chatKey
                ? {
                    ...c,
                    snippet: message.text ?? message.caption ?? c.snippet,
                    time: message.time,
                    unread: c.unread + 1,
                  }
                : c,
            )
            accounts = state.accounts.map((a) =>
              a.id === chat.accountId
                ? { ...a, unread: a.unread + 1, receivedToday: a.receivedToday + 1 }
                : a,
            )
          } else if (chat && message.direction === "out") {
            chats = state.chats.map((c) =>
              c.id === chatKey
                ? { ...c, snippet: `You: ${message.text ?? ""}`, time: message.time }
                : c,
            )
            accounts = state.accounts.map((a) =>
              a.id === chat.accountId ? { ...a, sentToday: a.sentToday + 1 } : a,
            )
          }
          return {
            messagesByChat: { ...state.messagesByChat, [chatKey]: [...list, message] },
            chats,
            accounts,
          }
        }),

      addMessages: (chatKey, messages) =>
        set((state) => {
          const existing = state.messagesByChat[chatKey] ?? []
          const seen = new Set(messages.map((m) => m.id))
          const extras = existing.filter((m) => !seen.has(m.id) && !m.id.startsWith("tmp-"))
          return {
            messagesByChat: {
              ...state.messagesByChat,
              [chatKey]: [...messages, ...extras],
            },
          }
        }),

      replaceMessage: (chatKey, tmpId, message) =>
        set((state) => {
          const list = state.messagesByChat[chatKey]
          if (!list) return {}
          if (list.some((m) => m.id === message.id)) {
            // the real message already arrived via push — just drop the tmp
            return {
              messagesByChat: {
                ...state.messagesByChat,
                [chatKey]: list.filter((m) => m.id !== tmpId),
              },
            }
          }
          return {
            messagesByChat: {
              ...state.messagesByChat,
              [chatKey]: list.map((m) => (m.id === tmpId ? message : m)),
            },
          }
        }),

      applyLiveMessage: (accountId, chatJid, message, key) => {
        void accountId
        void chatJid
        get().appendMessage(key, message)
      },

      applyMessageStatus: (chatKey, messageId, status) =>
        set((state) => {
          const list = state.messagesByChat[chatKey]
          if (!list) return {}
          const delivery = status >= 4 ? ("read" as const) : ("sent" as const)
          return {
            messagesByChat: {
              ...state.messagesByChat,
              [chatKey]: list.map((m) =>
                m.id === messageId && m.direction === "out" ? { ...m, delivery } : m,
              ),
            },
          }
        }),

      setHydrated: () => set({ hydrated: true }),
      reset: () =>
        set({
          tab: "vault",
          overlay: null,
          sheet: null,
          inboxFilter: "all",
          accounts: INITIAL_ACCOUNTS,
          chats: INITIAL_CHATS,
          messagesByChat: {},
          pairing: null,
          // theme + view + mode intentionally preserved — they are persisted
          // fields and resetting them would fight the persistence demo.
        }),
      resetToDemo: () => get().reset(),
    }),
    {
      name: "wault-proto-v1",
      version: 2,
      storage: createJSONStorage(() => localStorage),
      partialize: (state): PersistedWaultState => ({
        theme: state.theme,
        inboxFilter: state.inboxFilter,
        view: state.view,
        mode: state.mode,
      }),
      migrate: (persisted: unknown) => {
        const p = (persisted ?? {}) as Partial<PersistedWaultState>
        return { ...p, mode: p.mode === "prototype" ? ("prototype" as const) : ("app" as const) }
      },
      // Hydration happens manually after mount (page.tsx calls
      // useWaultStore.persist.rehydrate()), so the first client render
      // matches the SSR markup — no mismatch from persisted values.
      skipHydration: true,
      onRehydrateStorage: () => (state) => {
        state?.setHydrated()
      },
    },
  ),
)

/* ------------------------------------------------------------------ */
/* App shell                                                           */
/* ------------------------------------------------------------------ */

export function WaultApp({ fullBleed = false }: { fullBleed?: boolean }) {
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

  const [sheetHost, setSheetHost] = React.useState<HTMLDivElement | null>(null)
  const dark = theme === "dark"

  const totalUnread = React.useMemo(
    () => accounts.reduce((sum, a) => sum + a.unread, 0),
    [accounts],
  )

  const completePairing = (result: PairingResult) => {
    if (mode === "app" && live) {
      // Live mode: the account was created by the bridge at pairing start
      // and is already in the store — nothing to add here.
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

  let overlayNode: React.ReactNode = null
  if (overlay?.type === "conversation") {
    const chat = chats.find((c) => c.id === overlay.chatId)
    const account = chat ? accounts.find((a) => a.id === chat.accountId) : undefined
    if (chat && account) {
      overlayNode = (
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
  } else if (overlay?.type === "pairing") {
    overlayNode = (
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
    )
  }

  const body = (
    <>
      <PhoneThemeProvider dark={dark}>
          <div className="relative flex min-h-0 flex-1 flex-col">
            {/* Tab screens — fade + 8px shared-axis slide */}
            <AnimatePresence initial={false}>
              <motion.div
                key={tab}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={{ duration: 0.22, ease: "easeOut" }}
                className="absolute inset-0 flex flex-col"
              >
                {tab === "vault" ? (
                  <VaultScreen
                    accounts={accounts}
                    topRight={<AvatarCluster accounts={accounts} onOpen={() => setSheet("switcher")} />}
                    onOpenAccount={(accountId) => {
                      setInboxFilter(accountId)
                      setTab("inbox")
                    }}
                    onPair={openPairing}
                  />
                ) : null}
                {tab === "inbox" ? (
                  <InboxScreen
                    accounts={accounts}
                    chats={chats}
                    filter={inboxFilter}
                    onFilter={setInboxFilter}
                    onOpenChat={(chatId) => liveActions.openChat(chatId)}
                    onNewMessage={() => setSheet("newmessage")}
                    topRight={<AvatarCluster accounts={accounts} onOpen={() => setSheet("switcher")} />}
                  />
                ) : null}
                {tab === "settings" ? (
                  <SettingsScreen
                    accounts={accounts}
                    theme={theme}
                    onTheme={setTheme}
                    onOpenChannels={() => setSheet("channels")}
                    sheetHost={sheetHost}
                    channelsOpen={sheet === "channels"}
                    onCloseChannels={() => setSheet(null)}
                    topRight={<AvatarCluster accounts={accounts} onOpen={() => setSheet("switcher")} />}
                  />
                ) : null}
              </motion.div>
            </AnimatePresence>

            {/* Full-screen overlays — cover content + nav, keep gesture bar */}
            <AnimatePresence>{overlayNode}</AnimatePresence>

            {/* Bottom nav — hidden while an overlay is open */}
            <AnimatePresence>
              {!overlay ? (
                <motion.div
                  key="bottom-nav"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.15 }}
                >
                  <BottomNav tab={tab} onTab={setTab} unreadTotal={totalUnread} />
                </motion.div>
              ) : null}
            </AnimatePresence>
          </div>
      </PhoneThemeProvider>
    </>
  )

  const sheets = (
    <>
      {/* Sheets (vaul) — portaled into the shell */}
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

  if (fullBleed) {
    return (
      <>
        <div
          className={cn(
            "relative flex h-full min-h-0 w-full flex-col overflow-hidden",
            "bg-[#F7F7F5] dark:bg-[#0C0D0F]",
            dark && "dark",
          )}
          style={
            {
              "--wa-bg": dark ? "#0C0D0F" : "#F7F7F5",
              "--wa-hairline": dark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.07)",
            } as React.CSSProperties
          }
        >
          {body}
          {/* Portal host for vaul sheets */}
          <div ref={setSheetHost} className="contents" />
        </div>
        {sheets}
      </>
    )
  }

  return (
    <div className="my-10">
      <PhoneFrame dark={dark} sheetHostRef={setSheetHost}>{body}</PhoneFrame>
      {sheets}
    </div>
  )
}
