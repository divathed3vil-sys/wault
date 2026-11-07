/* eslint-disable react-hooks/rules-of-hooks */
/**
 * Wault Bridge — the WhatsApp session server.
 *
 * One process holds every office account as a lightweight multi-device
 * session (Baileys). Clients (web app now, Android "Office mode" later)
 * connect over socket.io and get a typed request/response + push API:
 *
 *   requests (ack): accounts:list · account:start · account:logout ·
 *                   account:remove · chats:list · chats:markRead ·
 *                   messages:list · message:send
 *   pushes:         accounts · qr · message · message:status · chats:refresh
 *
 * Persistence: per-account auth state under data/auth/<id>/, messages in
 * SQLite (bun:sqlite), account registry in data/accounts.json.
 */

import { createServer } from "http"
import { Server } from "socket.io"
import { Database } from "bun:sqlite"
import path from "path"
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "fs"
import QRCode from "qrcode"
import pino from "pino"
import makeWASocket, {
  Browsers,
  DisconnectReason,
  fetchLatestBaileysVersion,
  makeCacheableSignalKeyStore,
  useMultiFileAuthState,
} from "baileys"
import type { WASocket, WAMessage } from "baileys"

const PORT = 3031
const DATA_DIR = path.join(import.meta.dir, "data")
const AUTH_ROOT = path.join(DATA_DIR, "auth")
const ACCOUNTS_FILE = path.join(DATA_DIR, "accounts.json")
mkdirSync(AUTH_ROOT, { recursive: true })

const logger = pino({ level: "error" })
const log = (...args: unknown[]) => console.log("[bridge]", ...args)

/* ------------------------------------------------------------------ */
/* Types                                                               */
/* ------------------------------------------------------------------ */

type Status =
  | "disconnected"
  | "qr"
  | "connecting"
  | "connected"
  | "reconnecting"
  | "error"

interface WaultAccount {
  id: string
  name: string
  accent: string
  phone: string | null
  pushName: string | null
  status: Status
  createdAt: number
}

interface LiveMessage {
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

interface LiveChat {
  jid: string
  accountId: string
  name: string
  lastText: string | null
  lastFromMe: boolean
  lastTs: number
  kind: "dm" | "group"
  unread: number
}

interface Session {
  account: WaultAccount
  sock: WASocket | null
  qrAttempt: number
  wantConnected: boolean
  restarts: number
}

/* ------------------------------------------------------------------ */
/* Account registry                                                    */
/* ------------------------------------------------------------------ */

function loadAccounts(): WaultAccount[] {
  try {
    return JSON.parse(readFileSync(ACCOUNTS_FILE, "utf8")) as WaultAccount[]
  } catch {
    return []
  }
}

const accounts: WaultAccount[] = loadAccounts()
const sessions = new Map<string, Session>()

const saveAccounts = () =>
  writeFileSync(ACCOUNTS_FILE, JSON.stringify(accounts, null, 2))

const publicAccount = (a: WaultAccount) => ({
  id: a.id,
  name: a.name,
  accent: a.accent,
  phone: a.phone,
  pushName: a.pushName,
  status: a.status,
  createdAt: a.createdAt,
})

const emitAccounts = () => io.emit("accounts", accounts.map(publicAccount))

function setStatus(s: Session, status: Status) {
  s.account.status = status
  saveAccounts()
  emitAccounts()
}

const authDir = (id: string) => path.join(AUTH_ROOT, id)

/* ------------------------------------------------------------------ */
/* SQLite store                                                        */
/* ------------------------------------------------------------------ */

const db = new Database(path.join(DATA_DIR, "wault.sqlite"))
db.exec(`
  PRAGMA journal_mode = WAL;
  CREATE TABLE IF NOT EXISTS messages (
    id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    chat_jid TEXT NOT NULL,
    from_me INTEGER NOT NULL,
    sender_jid TEXT NOT NULL DEFAULT '',
    sender_name TEXT,
    text TEXT,
    kind TEXT NOT NULL DEFAULT 'text',
    status INTEGER NOT NULL DEFAULT 1,
    read_receipt INTEGER NOT NULL DEFAULT 0,
    timestamp INTEGER NOT NULL,
    PRIMARY KEY (account_id, chat_jid, id)
  );
  CREATE TABLE IF NOT EXISTS contacts (
    account_id TEXT NOT NULL,
    jid TEXT NOT NULL,
    name TEXT NOT NULL,
    PRIMARY KEY (account_id, jid)
  );
  CREATE INDEX IF NOT EXISTS idx_messages_chat_ts
    ON messages (account_id, chat_jid, timestamp DESC);
`)

const insertMessage = (m: {
  id: string
  accountId: string
  chatJid: string
  fromMe: boolean
  senderJid: string
  senderName: string | null
  text: string | null
  kind: string
  status: number
  readReceipt: number
  timestamp: number
}) =>
  db.run(
    `INSERT OR REPLACE INTO messages
       (id, account_id, chat_jid, from_me, sender_jid, sender_name, text, kind, status, read_receipt, timestamp)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      m.id,
      m.accountId,
      m.chatJid,
      m.fromMe ? 1 : 0,
      m.senderJid,
      m.senderName,
      m.text,
      m.kind,
      m.status,
      m.readReceipt,
      m.timestamp,
    ],
  )

const upsertContact = (accountId: string, jid: string, name: string) => {
  if (!jid || !name) return
  db.run(
    `INSERT OR REPLACE INTO contacts (account_id, jid, name) VALUES (?, ?, ?)`,
    [accountId, jid, name],
  )
}

const contactNames = (accountId: string): Map<string, string> => {
  const rows = db
    .query(`SELECT jid, name FROM contacts WHERE account_id = ?`)
    .all(accountId) as { jid: string; name: string }[]
  return new Map(rows.map((r) => [r.jid, r.name]))
}

const startOfDay = () => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}

/* ------------------------------------------------------------------ */
/* Message extraction                                                  */
/* ------------------------------------------------------------------ */

function extract(m: WAMessage): {
  text: string | null
  kind: "text" | "media"
  system: boolean
} {
  let c: any = m.message
  for (let depth = 0; depth < 3 && c; depth++) {
    if (c.conversation) return { text: c.conversation, kind: "text", system: false }
    if (c.extendedTextMessage?.text)
      return { text: c.extendedTextMessage.text, kind: "text", system: false }
    if (c.imageMessage)
      return { text: c.imageMessage.caption || "[Photo]", kind: "media", system: false }
    if (c.videoMessage)
      return { text: c.videoMessage.caption || "[Video]", kind: "media", system: false }
    if (c.stickerMessage) return { text: "[Sticker]", kind: "media", system: false }
    if (c.audioMessage)
      return {
        text: c.audioMessage.ptv ? "[Voice message]" : "[Audio]",
        kind: "media",
        system: false,
      }
    if (c.documentMessage)
      return {
        text: c.documentMessage.caption || c.documentMessage.fileName || "[Document]",
        kind: "media",
        system: false,
      }
    if (c.reactionMessage || c.protocolMessage)
      return { text: null, kind: "text", system: true }
    const next = c.ephemeralMessage?.message
      ?? c.viewOnceMessage?.message
      ?? c.viewOnceMessageV2?.message
      ?? c.documentWithCaptionMessage?.message
    if (next) {
      c = next
      continue
    }
    return { text: "[Unsupported message]", kind: "text", system: false }
  }
  return { text: "[Unsupported message]", kind: "text", system: false }
}

const isGroupJid = (jid: string) => jid.endsWith("@g.us")

/* ------------------------------------------------------------------ */
/* Session engine                                                      */
/* ------------------------------------------------------------------ */

async function startSession(accountId: string) {
  const s = sessions.get(accountId)
  if (!s) return
  s.wantConnected = true
  s.qrAttempt = 0
  setStatus(s, "connecting")

  const { state, saveCreds } = await useMultiFileAuthState(authDir(accountId))
  let version: number[] | undefined
  try {
    version = (await fetchLatestBaileysVersion()).version
  } catch {
    /* offline — Baileys falls back to its baked-in version */
  }

  const sock = makeWASocket({
    version,
    auth: {
      creds: state.creds,
      keys: makeCacheableSignalKeyStore(state.keys, logger),
    },
    logger,
    printQRInTerminal: false,
    browser: Browsers.macOS("Chrome"),
    markOnlineOnConnect: false,
    syncFullHistory: false,
  })
  s.sock = sock
  log(`session starting for ${accountId} (${s.account.name})`)

  sock.ev.on("creds.update", saveCreds)

  sock.ev.on("connection.update", async (u) => {
    if (u.qr) {
      s.qrAttempt++
      try {
        const dataUrl = await QRCode.toDataURL(u.qr, { margin: 1, width: 512 })
        io.emit("qr", { accountId, dataUrl, attempt: s.qrAttempt })
        setStatus(s, "qr")
        log(`QR ${s.qrAttempt} emitted for ${accountId}`)
      } catch {
        /* qr render failure — next attempt will retry */
      }
    }

    if (u.connection === "open") {
      s.qrAttempt = 0
      s.restarts = 0
      s.account.status = "connected"
      const uid = sock.user?.id ?? ""
      const digits = uid.split("@")[0]?.split(":")[0] ?? ""
      if (digits) s.account.phone = "+" + digits
      s.account.pushName = sock.user?.name ?? s.account.pushName ?? null
      saveAccounts()
      emitAccounts()
      log(`account ${accountId} CONNECTED as ${s.account.phone}`)
    }

    if (u.connection === "close") {
      s.sock = null
      const code = (u.lastDisconnect?.error as any)?.output?.statusCode
      log(`connection closed for ${accountId} (code ${code})`)

      if (code === DisconnectReason.loggedOut) {
        rmSync(authDir(accountId), { recursive: true, force: true })
        s.wantConnected = false
        setStatus(s, "disconnected")
        return
      }

      if (!s.wantConnected) {
        setStatus(s, "disconnected")
        return
      }

      // QR never scanned after 5 codes → stop and wait for the user.
      if (code === DisconnectReason.timedOut && s.qrAttempt >= 5) {
        s.wantConnected = false
        setStatus(s, "disconnected")
        return
      }

      setStatus(s, "reconnecting")
      const delay = Math.min(15000, 1000 * ++s.restarts)
      setTimeout(() => {
        const cur = sessions.get(accountId)
        if (cur && cur.wantConnected && !cur.sock) void startSession(accountId)
      }, delay)
    }
  })

  sock.ev.on("messages.upsert", ({ messages: msgs, type }) => {
    const fresh = type === "notify"
    let emitted = 0
    for (const m of msgs) {
      const jid = m.key.remoteJid
      const id = m.key.id
      if (!jid || !id || jid === "status@broadcast") continue
      const { text, kind, system } = extract(m)
      if (system) continue

      const fromMe = !!m.key.fromMe
      const senderJid = fromMe ? (sock.user?.id ?? "") : (m.key.participant ?? jid)
      const senderName = !fromMe && m.pushName ? m.pushName : null
      if (senderName) upsertContact(accountId, senderJid, senderName)

      const ts = Number(m.messageTimestamp) * 1000
      const status = fromMe ? 2 : 1
      insertMessage({
        id,
        accountId,
        chatJid: jid,
        fromMe,
        senderJid,
        senderName,
        text,
        kind,
        status,
        readReceipt: 0,
        timestamp: ts,
      })

      if (fresh) {
        io.emit("message", {
          accountId,
          message: {
            id,
            chatJid: jid,
            fromMe,
            senderJid,
            senderName,
            text,
            kind,
            timestamp: ts,
            status,
          } satisfies LiveMessage,
        })
        emitted++
      }
    }
    if (emitted > 0 || fresh === false) {
      // chat list changed (new message or history sync) → clients re-fetch
      io.emit("chats:refresh", { accountId })
    }
  })

  sock.ev.on("messages.update", (updates) => {
    for (const u of updates) {
      const st = (u.update as any)?.status
      if (typeof st !== "number") continue
      const chatJid = u.key.remoteJid
      const id = u.key.id
      if (!chatJid || !id) continue
      db.run(
        `UPDATE messages SET status = ? WHERE account_id = ? AND chat_jid = ? AND id = ?`,
        [st, accountId, chatJid, id],
      )
      io.emit("message:status", { accountId, chatJid, id, status: st })
    }
  })

  sock.ev.on("contacts.upsert", (cs: any[]) => {
    for (const c of cs) {
      const name = c?.name || c?.notify || c?.verifiedName
      if (c?.id && name) upsertContact(accountId, c.id, name)
    }
  })

  sock.ev.on("groups.upsert", (groups: any[]) => {
    for (const g of groups) {
      if (g?.id && g?.subject) upsertContact(accountId, g.id, g.subject)
    }
  })
}

/* ------------------------------------------------------------------ */
/* HTTP + socket.io                                                    */
/* ------------------------------------------------------------------ */

const httpServer = createServer((req, res) => {
  if (req.url === "/healthz") {
    res.writeHead(200, { "content-type": "application/json" })
    res.end(JSON.stringify({ ok: true, accounts: accounts.length }))
    return
  }
  res.writeHead(404)
  res.end()
})

const io = new Server(httpServer, {
  // DO NOT change the path — Caddy forwards on it
  path: "/",
  cors: { origin: "*", methods: ["GET", "POST"] },
  pingTimeout: 60000,
  pingInterval: 25000,
})

const hexOk = (v: unknown) => typeof v === "string" && /^#[0-9a-fA-F]{6}$/.test(v)
const strOk = (v: unknown, max: number) =>
  typeof v === "string" && v.trim().length > 0 && v.trim().length <= max

async function markChatRead(accountId: string, chatJid: string) {
  const unmarked = db
    .query(
      `SELECT id, sender_jid FROM messages
       WHERE account_id = ? AND chat_jid = ? AND from_me = 0 AND read_receipt = 0
       ORDER BY timestamp DESC LIMIT 100`,
    )
    .all(accountId, chatJid) as { id: string; sender_jid: string }[]

  if (unmarked.length > 0) {
    const sock = sessions.get(accountId)?.sock
    if (sock) {
      try {
        await sock.readMessages(
          unmarked.map((r) => ({
            remoteJid: chatJid,
            id: r.id,
            fromMe: false,
            participant: isGroupJid(chatJid) ? r.sender_jid : undefined,
          })),
        )
      } catch {
        /* read receipts are best-effort */
      }
    }
    db.run(
      `UPDATE messages SET read_receipt = 1
       WHERE account_id = ? AND chat_jid = ? AND from_me = 0 AND read_receipt = 0`,
      [accountId, chatJid],
    )
    io.emit("chats:refresh", { accountId })
  }
}

io.on("connection", (socket) => {
  log("client connected:", socket.id)
  socket.emit("accounts", accounts.map(publicAccount))

  socket.on("accounts:list", (_p, ack) => {
    ack?.(accounts.map(publicAccount))
  })

  socket.on("account:start", async (p: any, ack) => {
    if (!strOk(p?.name, 60) || !hexOk(p?.accent)) {
      ack?.({ ok: false, error: "name and accent are required" })
      return
    }
    if (accounts.length >= 4) {
      ack?.({ ok: false, error: "Device slots full (4 linked accounts)" })
      return
    }
    const id = "a" + Math.random().toString(36).slice(2, 8)
    const account: WaultAccount = {
      id,
      name: p.name.trim(),
      accent: p.accent,
      phone: null,
      pushName: null,
      status: "connecting",
      createdAt: Date.now(),
    }
    accounts.push(account)
    sessions.set(id, {
      account,
      sock: null,
      qrAttempt: 0,
      wantConnected: false,
      restarts: 0,
    })
    saveAccounts()
    emitAccounts()
    void startSession(id)
    ack?.({ ok: true, accountId: id })
  })

  socket.on("account:logout", async (p: any, ack) => {
    const s = sessions.get(p?.accountId)
    if (!s) {
      ack?.({ ok: false, error: "unknown account" })
      return
    }
    s.wantConnected = false
    try {
      await s.sock?.logout()
    } catch {
      /* already unlinked */
    }
    try {
      s.sock?.end(undefined)
    } catch {
      /* noop */
    }
    s.sock = null
    rmSync(authDir(p.accountId), { recursive: true, force: true })
    setStatus(s, "disconnected")
    log("account logged out:", p.accountId)
    ack?.({ ok: true })
  })

  socket.on("account:remove", async (p: any, ack) => {
    const idx = accounts.findIndex((a) => a.id === p?.accountId)
    if (idx === -1) {
      ack?.({ ok: false, error: "unknown account" })
      return
    }
    const s = sessions.get(p.accountId)
    if (s) {
      s.wantConnected = false
      try {
        await s.sock?.logout()
      } catch {
        /* noop */
      }
      try {
        s.sock?.end(undefined)
      } catch {
        /* noop */
      }
      sessions.delete(p.accountId)
    }
    rmSync(authDir(p.accountId), { recursive: true, force: true })
    db.run(`DELETE FROM messages WHERE account_id = ?`, [p.accountId])
    db.run(`DELETE FROM contacts WHERE account_id = ?`, [p.accountId])
    accounts.splice(idx, 1)
    saveAccounts()
    emitAccounts()
    ack?.({ ok: true })
  })

  socket.on("chats:list", async (p: any, ack) => {
    const accountId = p?.accountId
    if (!accounts.some((a) => a.id === accountId)) {
      ack?.({ ok: false, error: "unknown account" })
      return
    }
    const names = contactNames(accountId)
    const unreadRows = db
      .query(
        `SELECT chat_jid, COUNT(*) AS n FROM messages
         WHERE account_id = ? AND from_me = 0 AND read_receipt = 0
         GROUP BY chat_jid`,
      )
      .all(accountId) as { chat_jid: string; n: number }[]
    const unread = new Map(unreadRows.map((r) => [r.chat_jid, r.n]))

    const lastRows = db
      .query(
        `SELECT chat_jid, text, kind, from_me, timestamp FROM (
           SELECT *, ROW_NUMBER() OVER (
             PARTITION BY chat_jid ORDER BY timestamp DESC, rowid DESC
           ) AS rn
           FROM messages WHERE account_id = ?
         ) WHERE rn = 1 ORDER BY timestamp DESC`,
      )
      .all(accountId) as {
      chat_jid: string
      text: string | null
      kind: string
      from_me: number
      timestamp: number
    }[]

    const statsRow = db
      .query(
        `SELECT
           COALESCE(SUM(CASE WHEN from_me = 0 THEN 1 ELSE 0 END), 0) AS receivedToday,
           COALESCE(SUM(CASE WHEN from_me = 1 THEN 1 ELSE 0 END), 0) AS sentToday
         FROM messages WHERE account_id = ? AND timestamp >= ?`,
      )
      .get(accountId, startOfDay()) as {
      receivedToday: number
      sentToday: number
    }

    const chats: LiveChat[] = lastRows.map((r) => ({
      jid: r.chat_jid,
      accountId,
      name:
        names.get(r.chat_jid) ??
        (isGroupJid(r.chat_jid)
          ? r.chat_jid.split("@")[0]
          : "+" + (r.chat_jid.split("@")[0]?.split(":")[0] ?? r.chat_jid)),
      lastText: r.text,
      lastFromMe: r.from_me === 1,
      lastTs: r.timestamp,
      kind: isGroupJid(r.chat_jid) ? "group" : "dm",
      unread: unread.get(r.chat_jid) ?? 0,
    }))

    ack?.({ ok: true, chats, stats: statsRow })
  })

  socket.on("chats:markRead", async (p: any, ack) => {
    if (!accounts.some((a) => a.id === p?.accountId)) {
      ack?.({ ok: false, error: "unknown account" })
      return
    }
    await markChatRead(p.accountId, p.chatJid)
    ack?.({ ok: true })
  })

  socket.on("messages:list", async (p: any, ack) => {
    const accountId = p?.accountId
    const chatJid = p?.chatJid
    if (!accounts.some((a) => a.id === accountId) || typeof chatJid !== "string") {
      ack?.({ ok: false, error: "unknown account or chat" })
      return
    }
    const limit = Math.min(Math.max(Number(p?.limit) || 300, 1), 1000)
    const rows = db
      .query(
        `SELECT * FROM (
           SELECT * FROM messages WHERE account_id = ? AND chat_jid = ?
           ORDER BY timestamp DESC, rowid DESC LIMIT ?
         ) ORDER BY timestamp ASC, rowid ASC`,
      )
      .all(accountId, chatJid, limit) as any[]

    const messages: LiveMessage[] = rows.map((r) => ({
      id: r.id,
      chatJid: r.chat_jid,
      fromMe: r.from_me === 1,
      senderJid: r.sender_jid,
      senderName: r.sender_name,
      text: r.text,
      kind: r.kind === "media" ? "media" : "text",
      timestamp: r.timestamp,
      status: r.status,
    }))

    ack?.({ ok: true, messages })
    void markChatRead(accountId, chatJid)
  })

  socket.on("message:send", async (p: any, ack) => {
    const accountId = p?.accountId
    const chatJid = p?.chatJid
    if (!strOk(p?.text, 4096) || typeof chatJid !== "string") {
      ack?.({ ok: false, error: "chatJid and text are required" })
      return
    }
    const s = sessions.get(accountId)
    if (!s || s.account.status !== "connected" || !s.sock) {
      ack?.({ ok: false, error: "Account is not connected" })
      return
    }
    try {
      const text = p.text.trim()
      const sent = await s.sock.sendMessage(chatJid, { text })
      const id = sent?.key?.id ?? "local-" + Math.random().toString(36).slice(2)
      const ts = Number(sent?.messageTimestamp ?? Math.floor(Date.now() / 1000)) * 1000
      const message: LiveMessage = {
        id,
        chatJid,
        fromMe: true,
        senderJid: s.sock.user?.id ?? "",
        senderName: null,
        text,
        kind: "text",
        timestamp: ts,
        status: 2,
      }
      insertMessage({
        id,
        accountId,
        chatJid,
        fromMe: true,
        senderJid: message.senderJid,
        senderName: null,
        text,
        kind: "text",
        status: 2,
        readReceipt: 1,
        timestamp: ts,
      })
      io.emit("message", { accountId, message })
      ack?.({ ok: true, message })
      log(`sent ${id} via ${accountId} → ${chatJid}`)
    } catch (e: any) {
      log("send failed:", e?.message)
      ack?.({ ok: false, error: e?.message ?? "send failed" })
    }
  })
})

/* ------------------------------------------------------------------ */
/* Boot                                                                */
/* ------------------------------------------------------------------ */

for (const a of accounts) {
  sessions.set(a.id, {
    account: a,
    sock: null,
    qrAttempt: 0,
    wantConnected: false,
    restarts: 0,
  })
  const hasCreds = existsSync(path.join(authDir(a.id), "creds.json"))
  if (hasCreds) {
    void startSession(a.id) // auto-reconnect known devices
  } else {
    a.status = "disconnected"
  }
}
saveAccounts()
emitAccounts()

httpServer.listen(PORT, () => {
  log(`Wault Bridge listening on :${PORT} — ${accounts.length} account(s) registered`)
})

process.on("SIGTERM", () => {
  log("SIGTERM — closing sessions")
  for (const s of sessions.values()) {
    try {
      s.sock?.end(undefined)
    } catch {
      /* noop */
    }
  }
  httpServer.close(() => process.exit(0))
  setTimeout(() => process.exit(0), 1500)
})

process.on("SIGINT", () => {
  log("SIGINT — closing sessions")
  for (const s of sessions.values()) {
    try {
      s.sock?.end(undefined)
    } catch {
      /* noop */
    }
  }
  httpServer.close(() => process.exit(0))
  setTimeout(() => process.exit(0), 1500)
})
