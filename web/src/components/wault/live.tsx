"use client"

/**
 * Wault live layer — connects the UI to the Wault Bridge.
 *
 * WaultLiveProvider is mounted once (page level). It talks to the bridge,
 * maps wire types into the store's UI types and keeps the zustand store in
 * sync while the page is in "app" mode. When the bridge is unreachable the
 * app keeps running in demo mode on the mock data from data.ts.
 */

import * as React from "react"

import {
  bridge,
  type BridgeAccount,
  type BridgeChat,
  type BridgeMessage,
  type BridgeMessagePayload,
  type BridgeStats,
} from "@/lib/wault-bridge"

import type { Account, AccountStatus, Chat, Message } from "./data"
import { useWaultStore } from "./wault-app"

/* ------------------------------------------------------------------ */
/* Keys + time formatting                                              */
/* ------------------------------------------------------------------ */

export const chatKey = (accountId: string, chatJid: string) => `${accountId}|${chatJid}`

export function parseChatKey(key: string): { accountId: string; chatJid: string } {
  const idx = key.indexOf("|")
  if (idx === -1) return { accountId: "", chatJid: key }
  return { accountId: key.slice(0, idx), chatJid: key.slice(idx + 1) }
}

export function fmtMsgTime(ts: number): string {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`
}

export function fmtChatTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return fmtMsgTime(ts)
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return "Yesterday"
  return d.toLocaleDateString(undefined, { day: "numeric", month: "short" })
}

/* ------------------------------------------------------------------ */
/* Wire → UI mapping                                                   */
/* ------------------------------------------------------------------ */

function mapStatus(status: BridgeAccount["status"]): AccountStatus {
  if (status === "connected") return "connected"
  if (status === "disconnected" || status === "error") return "disconnected"
  return "reconnecting" // qr / connecting / reconnecting
}

function mapAccount(a: BridgeAccount, existing?: Account): Account {
  return {
    id: a.id,
    name: a.name,
    phone: a.phone ?? existing?.phone ?? "—",
    accent: a.accent,
    status: mapStatus(a.status),
    unread: existing?.unread ?? 0,
    receivedToday: existing?.receivedToday ?? 0,
    sentToday: existing?.sentToday ?? 0,
    lastActive: existing?.lastActive ?? "—",
  }
}

function mapChat(c: BridgeChat): Chat {
  return {
    id: chatKey(c.accountId, c.jid),
    accountId: c.accountId,
    name: c.name,
    snippet: (c.lastFromMe ? "You: " : "") + (c.lastText ?? ""),
    time: fmtChatTime(c.lastTs),
    unread: c.unread,
    pinned: false,
    label: null,
    group: c.kind === "group",
  }
}

function mapMessage(m: BridgeMessage): Message {
  const media = m.kind === "media"
  return {
    id: m.id,
    direction: m.fromMe ? "out" : "in",
    time: fmtMsgTime(m.timestamp),
    kind: media ? "media" : "text",
    text: media ? undefined : m.text ?? "",
    caption: media ? m.text ?? "[Media]" : undefined,
    delivery: m.fromMe ? (m.status >= 4 ? "read" : "sent") : undefined,
  }
}

/* ------------------------------------------------------------------ */
/* Chat sync                                                           */
/* ------------------------------------------------------------------ */

/** Pull the chat list for every known account and swap it into the store. */
async function refreshAllChats() {
  const st = useWaultStore.getState()
  if (st.mode !== "app" || !st.live) return
  try {
    const res = await bridge.request<BridgeAccount[]>("accounts:list")
    if (!Array.isArray(res)) return
    const all: BridgeChat[] = []
    const stats: Record<string, BridgeStats> = {}
    for (const a of res) {
      const r = await bridge.request<{ ok: boolean; chats: BridgeChat[]; stats: BridgeStats }>(
        "chats:list",
        { accountId: a.id },
      )
      if (r.ok) {
        all.push(...r.chats)
        stats[a.id] = r.stats
      }
    }
    all.sort((x, y) => y.lastTs - x.lastTs)
    useWaultStore.getState().setLiveChats(all.map(mapChat), stats)
  } catch {
    /* bridge hiccup — next refresh retries */
  }
}

/* ------------------------------------------------------------------ */
/* Live actions (module level — usable from anywhere)                  */
/* ------------------------------------------------------------------ */

export const liveActions = {
  async startPairing(name: string, accentHex: string) {
    const res = await bridge.request<{ ok: boolean; accountId?: string; error?: string }>(
      "account:start",
      { name, accent: accentHex },
    )
    if (res.ok && res.accountId) {
      useWaultStore
        .getState()
        .setPairing({ accountId: res.accountId, qrDataUrl: null, linked: false })
    }
    return res
  },

  async logoutAccount(accountId: string) {
    try {
      await bridge.request("account:logout", { accountId })
    } catch {
      /* offline */
    }
  },

  async removeAccount(accountId: string) {
    try {
      await bridge.request("account:remove", { accountId })
    } catch {
      /* offline */
    }
  },

  openChat(chatId: string) {
    const store = useWaultStore.getState()
    store.openConversation(chatId)
    if (store.mode === "app" && store.live) {
      const { accountId, chatJid } = parseChatKey(chatId)
      void bridge
        .request<{ ok: boolean; messages: BridgeMessage[] }>("messages:list", {
          accountId,
          chatJid,
        })
        .then((res) => {
          if (res.ok) {
            useWaultStore.getState().addMessages(chatId, res.messages.map(mapMessage))
          }
        })
        .catch(() => {})
    }
  },

  async sendMessage(chatId: string, text: string): Promise<boolean> {
    const store = useWaultStore.getState()
    if (store.mode !== "app" || !store.live) return false
    const { accountId, chatJid } = parseChatKey(chatId)
    const tmpId = `tmp-${Date.now()}-${Math.round(Math.random() * 1e6)}`
    useWaultStore.getState().appendMessage(chatId, {
      id: tmpId,
      direction: "out",
      time: fmtMsgTime(Date.now()),
      kind: "text",
      text,
      delivery: "sent",
    })
    try {
      const res = await bridge.request<{ ok: boolean; message?: BridgeMessage; error?: string }>(
        "message:send",
        { accountId, chatJid, text },
      )
      if (res.ok && res.message) {
        useWaultStore.getState().replaceMessage(chatId, tmpId, mapMessage(res.message))
        return true
      }
      return false
    } catch {
      return false
    }
  },
}

/* ------------------------------------------------------------------ */
/* Provider                                                            */
/* ------------------------------------------------------------------ */

export function WaultLiveProvider({ children }: { children: React.ReactNode }) {
  const mode = useWaultStore((s) => s.mode)
  const setLive = useWaultStore((s) => s.setLive)

  React.useEffect(() => {
    let disposed = false
    const st = () => useWaultStore.getState()

    const hydrate = async () => {
      if (disposed || st().mode !== "app" || !bridge.connected) return
      try {
        const list = await bridge.request<BridgeAccount[]>("accounts:list")
        if (disposed || !Array.isArray(list) || st().mode !== "app") return
        st().seedLiveAccounts(list.map((a) => mapAccount(a)))
        for (const a of list) {
          if (a.status === "connected") void refreshAllChats()
        }
      } catch {
        /* offline */
      }
    }

    const unsubs = [
      bridge.on("bridge:up", () => {
        setLive(true)
        void hydrate()
      }),
      bridge.on("bridge:down", () => setLive(false)),
      bridge.on("accounts", (list: BridgeAccount[]) => {
        if (st().mode !== "app") return
        st().applyAccounts(
          list.map((a) => mapAccount(a, st().accounts.find((x) => x.id === a.id))),
        )
      }),
      bridge.on("qr", (p: { accountId: string; dataUrl: string; attempt: number }) => {
        const pairing = st().pairing
        if (st().mode !== "app" || !pairing || pairing.accountId !== p.accountId) return
        st().setPairing({ ...pairing, qrDataUrl: p.dataUrl })
      }),
      bridge.on("message", (p: BridgeMessagePayload) => {
        if (st().mode !== "app") return
        st().applyLiveMessage(
          p.accountId,
          p.message.chatJid,
          mapMessage(p.message),
          chatKey(p.accountId, p.message.chatJid),
        )
      }),
      bridge.on(
        "message:status",
        (p: { accountId: string; chatJid: string; id: string; status: number }) => {
          if (st().mode !== "app") return
          st().applyMessageStatus(chatKey(p.accountId, p.chatJid), p.id, p.status)
        },
      ),
      bridge.on("chats:refresh", () => {
        if (st().mode !== "app" || !st().live) return
        void refreshAllChats()
      }),
    ]

    void bridge.connect().then((ok) => {
      if (disposed) return
      setLive(ok)
      if (ok) void hydrate()
    })

    return () => {
      disposed = true
      for (const off of unsubs) off()
    }
  }, [setLive])

  // Leaving app mode → return the store to the demo dataset (the prototype
  // is a self-contained showcase); entering app mode → re-seed live accounts
  // and chats from the bridge.
  React.useEffect(() => {
    if (mode === "prototype") {
      useWaultStore.getState().resetToDemo()
      return
    }
    if (!bridge.connected) return
    void bridge
      .request<BridgeAccount[]>("accounts:list")
      .then((list) => {
        if (!Array.isArray(list)) return
        const s = useWaultStore.getState()
        if (s.mode !== "app") return
        s.seedLiveAccounts(list.map((a) => mapAccount(a)))
        void refreshAllChats()
      })
      .catch(() => {})
  }, [mode])

  return <>{children}</>
}
