"use client"

import * as React from "react"
import { AnimatePresence, motion } from "framer-motion"
import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import {
  INITIAL_ACCOUNTS,
  INITIAL_CHATS,
  type Account,
  type Chat,
} from "./data"
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
  setHydrated: () => void
  reset: () => void
}

/** Fields written to localStorage — everything else stays ephemeral. */
type PersistedWaultState = Pick<WaultStore, "theme" | "inboxFilter" | "view">

export const useWaultStore = create<WaultStore>()(
  persist(
    (set) => ({
      tab: "vault",
      overlay: null,
      sheet: null,
      inboxFilter: "all",
      accounts: INITIAL_ACCOUNTS,
      chats: INITIAL_CHATS,
      theme: "light",
      view: "phone",
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
      setHydrated: () => set({ hydrated: true }),
      reset: () =>
        set({
          tab: "vault",
          overlay: null,
          sheet: null,
          inboxFilter: "all",
          accounts: INITIAL_ACCOUNTS,
          chats: INITIAL_CHATS,
          // theme + view intentionally preserved — they are persisted fields
          // and resetting them would fight the persistence demo.
        }),
    }),
    {
      name: "wault-proto-v1",
      version: 1,
      storage: createJSONStorage(() => localStorage),
      partialize: (state): PersistedWaultState => ({
        theme: state.theme,
        inboxFilter: state.inboxFilter,
        view: state.view,
      }),
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

export function WaultApp() {
  const tab = useWaultStore((s) => s.tab)
  const overlay = useWaultStore((s) => s.overlay)
  const sheet = useWaultStore((s) => s.sheet)
  const inboxFilter = useWaultStore((s) => s.inboxFilter)
  const accounts = useWaultStore((s) => s.accounts)
  const chats = useWaultStore((s) => s.chats)
  const theme = useWaultStore((s) => s.theme)
  const setTab = useWaultStore((s) => s.setTab)
  const openConversation = useWaultStore((s) => s.openConversation)
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
        <ConversationScreen key={chat.id} chat={chat} account={account} onBack={closeOverlay} />
      )
    }
  } else if (overlay?.type === "pairing") {
    overlayNode = <PairingFlow onClose={closeOverlay} onComplete={completePairing} />
  }

  return (
    <div className="my-10">
      <PhoneFrame dark={dark} sheetHostRef={setSheetHost}>
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
                    onOpenChat={openConversation}
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
      </PhoneFrame>

      {/* Sheets (vaul) — portaled into the phone frame */}
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
    </div>
  )
}
