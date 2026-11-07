/**
 * Wault v2 — design-contract mock data.
 * Pure data + deterministic helpers only (no React, no randomness, no clocks):
 * everything here must render identically on server and client.
 */

export type AccountStatus = "connected" | "reconnecting"

export interface Account {
  id: string
  name: string
  phone: string
  /** Accent hex, applied inline since it is dynamic per account. */
  accent: string
  status: AccountStatus
  unread: number
  receivedToday: number
  sentToday: number
  lastActive: string
}

export interface Chat {
  id: string
  accountId: string
  name: string
  snippet: string
  time: string
  unread: number
  pinned: boolean
  label: string | null
  group?: boolean
}

export interface Message {
  id: string
  direction: "in" | "out"
  time: string
  kind: "text" | "media"
  text?: string
  caption?: string
  /** Outgoing only: "sent" renders a single check, "read" renders double checks. */
  delivery?: "sent" | "read"
}

export interface AccentOption {
  id: string
  label: string
  hex: string
}

/* ------------------------------------------------------------------ */
/* Palette                                                             */
/* ------------------------------------------------------------------ */

export const BRAND = {
  light: "#059669",
  dark: "#34D399",
} as const

export const ACCENTS: AccentOption[] = [
  { id: "emerald", label: "Emerald", hex: "#10B981" },
  { id: "amber", label: "Amber", hex: "#F59E0B" },
  { id: "rose", label: "Rose", hex: "#F43F5E" },
  { id: "violet", label: "Violet", hex: "#8B5CF6" },
  { id: "teal", label: "Teal", hex: "#14B8A6" },
]

export const DEVICE_SLOTS = 4

/* ------------------------------------------------------------------ */
/* Mock data                                                           */
/* ------------------------------------------------------------------ */

export const INITIAL_ACCOUNTS: Account[] = [
  {
    id: "a1",
    name: "Sales · Aadhil",
    phone: "+94 77 214 8890",
    accent: "#10B981",
    status: "connected",
    unread: 9,
    receivedToday: 48,
    sentToday: 126,
    lastActive: "2m ago",
  },
  {
    id: "a2",
    name: "Support · Nethmi",
    phone: "+94 76 880 1123",
    accent: "#F43F5E",
    status: "connected",
    unread: 4,
    receivedToday: 61,
    sentToday: 88,
    lastActive: "just now",
  },
  {
    id: "a3",
    name: "Ops · Dilan",
    phone: "+94 71 553 2047",
    accent: "#F59E0B",
    status: "reconnecting",
    unread: 2,
    receivedToday: 17,
    sentToday: 9,
    lastActive: "1h ago",
  },
]

export const INITIAL_CHATS: Chat[] = [
  {
    id: "c1",
    accountId: "a1",
    name: "Malith Fernando",
    snippet: "Sure, I'll confirm the order by 4 pm",
    time: "16:05",
    unread: 3,
    pinned: true,
    label: "New order",
  },
  {
    id: "c2",
    accountId: "a2",
    name: "Hashini Perera",
    snippet: "The replacement unit was delivered, thank you",
    time: "15:52",
    unread: 1,
    pinned: true,
    label: "Warranty",
  },
  {
    id: "c3",
    accountId: "a1",
    name: "Ceylon Traders",
    snippet: "Kasun: Van left the Kandy warehouse",
    time: "15:40",
    unread: 2,
    pinned: false,
    label: "Dispatch",
    group: true,
  },
  {
    id: "c4",
    accountId: "a2",
    name: "Tharindu Silva",
    snippet: "You: Can you share the invoice PDF again?",
    time: "15:21",
    unread: 0,
    pinned: false,
    label: null,
  },
  {
    id: "c5",
    accountId: "a3",
    name: "Nadeesha Wickrama",
    snippet: "Schedule changed to Tuesday 10 am",
    time: "14:48",
    unread: 2,
    pinned: false,
    label: "Field ops",
  },
  {
    id: "c6",
    accountId: "a1",
    name: "Ruwan Jayasuriya",
    snippet: "You: Sent the catalogue. Let me know",
    time: "13:30",
    unread: 0,
    pinned: false,
    label: null,
  },
  {
    id: "c7",
    accountId: "a2",
    name: "Sanduni Bandara",
    snippet: "Thank you, that solved it",
    time: "12:12",
    unread: 0,
    pinned: false,
    label: null,
  },
  {
    id: "c8",
    accountId: "a3",
    name: "Colombo Logistics",
    snippet: "Waybill CL-88231 has been issued",
    time: "11:05",
    unread: 0,
    pinned: false,
    label: "Delivery",
  },
  {
    id: "c9",
    accountId: "a1",
    name: "Ishara Gunawardena",
    snippet: "Is the studio available on Friday?",
    time: "Yesterday",
    unread: 0,
    pinned: false,
    label: null,
  },
]

export const QUICK_REPLIES = [
  "Thank you for your order",
  "Delivery is on the way",
  "Could you share your address?",
] as const

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

/** Convert "#RRGGBB" + alpha into an rgba() string for inline tinting. */
export function withAlpha(hex: string, alpha: number): string {
  const h = hex.replace("#", "")
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** "Malith Fernando" -> "MF", "Sales · Aadhil" -> "SA". */
export function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.charAt(0) ?? "W"
  const last = parts.length > 1 ? parts[parts.length - 1]?.charAt(0) ?? "" : ""
  return (first + last).toUpperCase()
}

/** "Sales · Aadhil" -> "Sales" (short label for chips). */
export function accountShortName(name: string): string {
  return name.split(" · ")[0] ?? name
}

/**
 * Deterministic conversation script per chat. c1 is the fully scripted
 * showcase; every other chat gets a generic 4-5 message variant built
 * from its snippet.
 */
export function buildConversation(chat: Chat): Message[] {
  if (chat.id === "c1") {
    return [
      { id: "c1-m1", direction: "in", time: "09:12", kind: "text", text: "Hi, is the A4 paper carton price still 4,800?" },
      { id: "c1-m2", direction: "out", time: "09:14", kind: "text", text: "Yes, it is. Bulk orders above 10 cartons get 5 percent off", delivery: "read" },
      { id: "c1-m3", direction: "in", time: "09:15", kind: "text", text: "Great. I need 12 cartons delivered to Nugegoda" },
      { id: "c1-m4", direction: "out", time: "09:20", kind: "media", caption: "Here is the updated invoice", delivery: "read" },
      { id: "c1-m5", direction: "in", time: "09:22", kind: "text", text: "Perfect, payment will go out today" },
      { id: "c1-m6", direction: "out", time: "09:23", kind: "text", text: "Thank you! Delivery will reach you before 6 pm", delivery: "read" },
    ]
  }
  const own = chat.snippet.startsWith("You: ")
  const body = own ? chat.snippet.slice(5) : chat.snippet
  if (own) {
    return [
      { id: `${chat.id}-g1`, direction: "in", time: "09:41", kind: "text", text: "Hey, do you have a quick moment?" },
      { id: `${chat.id}-g2`, direction: "out", time: "09:42", kind: "text", text: body, delivery: "read" },
      { id: `${chat.id}-g3`, direction: "in", time: "09:44", kind: "text", text: "Yes, that works on my side" },
      { id: `${chat.id}-g4`, direction: "out", time: "09:45", kind: "text", text: "Great, I will follow up once it is done", delivery: "read" },
      { id: `${chat.id}-g5`, direction: "in", time: "09:47", kind: "text", text: "Thanks, appreciate it" },
    ]
  }
  return [
    { id: `${chat.id}-g1`, direction: "in", time: "09:41", kind: "text", text: body },
    { id: `${chat.id}-g2`, direction: "out", time: "09:43", kind: "text", text: "Noted, thank you for the update", delivery: "read" },
    { id: `${chat.id}-g3`, direction: "in", time: "09:46", kind: "text", text: "Let me know if anything changes on your side" },
    { id: `${chat.id}-g4`, direction: "out", time: "09:48", kind: "text", text: "Will do. I will keep you posted", delivery: "read" },
  ]
}

/* ------------------------------------------------------------------ */
/* Fake QR code — hardcoded literal (hydration-safe, no randomness).   */
/* 21x21 modules with finder patterns, timing lines and a dark module. */
/* ------------------------------------------------------------------ */

const QR_ROWS = [
  "111111100000001111111",
  "100000100011101000001",
  "101110101000101011101",
  "101110101100001011101",
  "101110101101001011101",
  "100000100011001000001",
  "111111101010101111111",
  "000000001100100000000",
  "010000100101010110110",
  "110000000111001011111",
  "100111101101010000011",
  "110100001000110000001",
  "111010101101010000000",
  "000000001111100101001",
  "111111101000100111111",
  "100000100111110101010",
  "101110101101000110001",
  "101110101100000001111",
  "101110100000001001111",
  "100000101011011110011",
  "111111100100011111010",
] as const

export const QR_MATRIX: boolean[][] = QR_ROWS.map((row) =>
  row.split("").map((ch) => ch === "1"),
)
