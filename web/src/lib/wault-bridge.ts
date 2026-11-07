"use client"

/**
 * Wault Bridge client — typed socket.io wrapper.
 *
 * Connects through the Caddy gateway (relative URL + XTransformPort, never
 * a port in the URL) and exposes request/response plus push events.
 * If the bridge is offline the app falls back to demo (mock) mode.
 */

import { io, type Socket } from "socket.io-client"

export const BRIDGE_PORT = 3031

export type BridgeAccountStatus =
  | "disconnected"
  | "qr"
  | "connecting"
  | "connected"
  | "reconnecting"
  | "error"

export interface BridgeAccount {
  id: string
  name: string
  accent: string
  phone: string | null
  pushName: string | null
  status: BridgeAccountStatus
  createdAt: number
}

export interface BridgeChat {
  jid: string
  accountId: string
  name: string
  lastText: string | null
  lastFromMe: boolean
  lastTs: number
  kind: "dm" | "group"
  unread: number
}

export interface BridgeStats {
  receivedToday: number
  sentToday: number
}

export interface BridgeMessage {
  id: string
  chatJid: string
  fromMe: boolean
  senderJid: string
  senderName: string | null
  text: string | null
  kind: "text" | "media"
  timestamp: number
  status: number
}

export interface BridgeQrPayload {
  accountId: string
  dataUrl: string
  attempt: number
}

export interface BridgeMessagePayload {
  accountId: string
  message: BridgeMessage
}

export interface BridgeStatusPayload {
  accountId: string
  chatJid: string
  id: string
  status: number
}

type Listener = (payload: any) => void

const FORWARDED_EVENTS = ["accounts", "qr", "message", "message:status", "chats:refresh"]
const LOCAL_EVENTS = ["bridge:up", "bridge:down", ...FORWARDED_EVENTS]

class BridgeClient {
  private socket: Socket | null = null
  private connecting: Promise<boolean> | null = null
  private listeners = new Map<string, Set<Listener>>()
  connected = false

  connect(): Promise<boolean> {
    if (this.connected && this.socket) return Promise.resolve(true)
    if (this.connecting) return this.connecting

    this.connecting = new Promise<boolean>((resolve) => {
      const socket = io(`/?XTransformPort=${BRIDGE_PORT}`, {
        transports: ["websocket", "polling"],
        reconnection: true,
        reconnectionDelay: 1500,
        reconnectionDelayMax: 8000,
        timeout: 8000,
      })
      this.socket = socket

      const timer = window.setTimeout(() => {
        if (!this.connected) resolve(false) // stays offline → demo mode
      }, 6000)

      socket.on("connect", () => {
        this.connected = true
        window.clearTimeout(timer)
        this.emitLocal("bridge:up", null)
        this.connecting = null
        resolve(true)
      })

      socket.on("disconnect", () => {
        this.connected = false
        this.emitLocal("bridge:down", null)
      })

      socket.io.on("reconnect", () => {
        this.connected = true
        this.emitLocal("bridge:up", null)
      })

      for (const event of FORWARDED_EVENTS) {
        socket.on(event, (payload) => this.emitLocal(event, payload))
      }
    })

    return this.connecting
  }

  on(event: string, cb: Listener): () => void {
    if (!LOCAL_EVENTS.includes(event)) return () => {}
    let set = this.listeners.get(event)
    if (!set) {
      set = new Set()
      this.listeners.set(event, set)
    }
    set.add(cb)
    return () => {
      set.delete(cb)
    }
  }

  request<T = any>(event: string, payload?: unknown, timeoutMs = 15000): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      const socket = this.socket
      if (!socket || !this.connected) {
        reject(new Error("bridge offline"))
        return
      }
      const timer = window.setTimeout(() => reject(new Error("bridge timeout")), timeoutMs)
      socket.emit(event, payload, (res: T) => {
        window.clearTimeout(timer)
        resolve(res)
      })
    })
  }

  disconnect() {
    this.socket?.disconnect()
    this.socket = null
    this.connected = false
    this.connecting = null
  }

  private emitLocal(event: string, payload: any) {
    const set = this.listeners.get(event)
    if (!set) return
    for (const cb of set) cb(payload)
  }
}

export const bridge = new BridgeClient()
