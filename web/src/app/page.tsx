"use client"

import * as React from "react"
import { AnimatePresence, motion } from "framer-motion"
import { Monitor, Moon, RotateCcw, ShieldCheck, Smartphone, Sun } from "lucide-react"

import { cn } from "@/lib/utils"
import { Toaster } from "@/components/ui/sonner"
import { DesignSystemStrip } from "@/components/wault/design-system-strip"
import { WaultLiveProvider } from "@/components/wault/live"
import { WebShell } from "@/components/wault/web-shell"
import { useWaultStore, WaultApp, type WaultPageMode, type WaultView } from "@/components/wault/wault-app"
import { useIsMobile } from "@/hooks/use-mobile"

export default function Home() {
  const mode = useWaultStore((s) => s.mode)
  const setMode = useWaultStore((s) => s.setMode)
  const hydrated = useWaultStore((s) => s.hydrated)
  const isMobile = useIsMobile()

  // The store persists via localStorage with skipHydration: true, so it is
  // rehydrated after mount — the first client render matches the server
  // markup and the placeholder below resolves within milliseconds.
  React.useEffect(() => {
    void useWaultStore.persist.rehydrate()
  }, [])

  return (
    <WaultLiveProvider>
      {!hydrated ? (
        <div className="flex min-h-[100dvh] items-center justify-center bg-[#F3F3F1]">
          <div
            aria-hidden
            className="h-10 w-10 animate-spin rounded-full border-2 border-black/10 border-t-[#059669]"
          />
        </div>
      ) : (
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={mode}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
          >
            {mode === "app" ? (
              <LiveApp
                isMobile={isMobile}
                onShowcase={() => setMode("prototype")}
              />
            ) : (
              <PrototypeShowcase onApp={() => setMode("app")} />
            )}
          </motion.div>
        </AnimatePresence>
      )}
      <Toaster position="top-center" />
    </WaultLiveProvider>
  )
}

/* ------------------------------------------------------------------ */
/* Mode switcher — shared by both headers                              */
/* ------------------------------------------------------------------ */

function ModeSwitch({
  mode,
  onChange,
}: {
  mode: WaultPageMode
  onChange: (mode: WaultPageMode) => void
}) {
  const options: { id: WaultPageMode; label: string; icon: typeof Smartphone }[] = [
    { id: "app", label: "Live app", icon: Smartphone },
    { id: "prototype", label: "Showcase", icon: Monitor },
  ]
  return (
    <div
      role="group"
      aria-label="Page mode"
      className="flex h-8 items-center gap-0.5 rounded-full border border-black/[0.08] bg-white p-0.5"
    >
      {options.map((option) => {
        const Icon = option.icon
        const active = mode === option.id
        return (
          <button
            key={option.id}
            type="button"
            onClick={() => onChange(option.id)}
            aria-pressed={active}
            className={cn(
              "flex h-7 items-center gap-1.5 rounded-full px-2.5 text-[12px] font-medium transition-colors",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
              active ? "bg-[#17181A] text-white" : "text-[#6B6F76] hover:text-[#17181A]",
            )}
          >
            <Icon size={14} aria-hidden />
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* Live app — the actual Wault web client (bridge-backed)              */
/* ------------------------------------------------------------------ */

function LiveApp({
  isMobile,
  onShowcase,
}: {
  isMobile: boolean
  onShowcase: () => void
}) {
  const live = useWaultStore((s) => s.live)
  const theme = useWaultStore((s) => s.theme)
  const toggleTheme = useWaultStore((s) => s.toggleTheme)

  return (
    <div className="flex h-[100dvh] flex-col bg-[#F3F3F1] text-[#17181A]">
      <header className="flex h-12 shrink-0 items-center gap-2.5 border-b border-black/[0.06] bg-white px-3">
        <span
          className="grid h-7 w-7 shrink-0 place-items-center rounded-lg"
          style={{ backgroundColor: "rgba(16,185,129,0.14)" }}
        >
          <ShieldCheck size={16} aria-hidden className="text-[#059669]" />
        </span>
        <span className="hidden text-[12px] font-semibold tracking-[0.18em] sm:inline">
          WAULT
        </span>
        <span
          className="hidden items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-medium sm:flex"
          style={{
            backgroundColor: live ? "rgba(16,185,129,0.12)" : "rgba(245,158,11,0.14)",
            color: live ? "#047857" : "#B45309",
          }}
        >
          <motion.span
            className="h-1.5 w-1.5 rounded-full"
            style={{ backgroundColor: live ? "#10B981" : "#F59E0B" }}
            animate={live ? { opacity: [1, 0.4, 1] } : { opacity: 1 }}
            transition={live ? { duration: 2, repeat: Infinity, ease: "easeInOut" } : undefined}
            aria-hidden
          />
          {live ? "Live · bridge connected" : "Demo · bridge offline"}
        </span>
        <span className="flex-1" />
        <ModeSwitch mode="app" onChange={(m) => m === "prototype" && onShowcase()} />
        <button
          type="button"
          onClick={toggleTheme}
          aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
          className={cn(
            "grid h-9 w-9 place-items-center rounded-full border border-black/[0.08] bg-white text-[#17181A] transition-colors",
            "hover:bg-black/[0.03] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
          )}
        >
          {theme === "dark" ? <Sun size={16} aria-hidden /> : <Moon size={16} aria-hidden />}
        </button>
      </header>
      <main className="min-h-0 flex-1">
        {isMobile ? <WaultApp fullBleed /> : <WebShell variant="app" />}
      </main>
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* Prototype showcase — the original Phase 0 design contract           */
/* ------------------------------------------------------------------ */

type JumpKey = "vault" | "inbox" | "conversation" | "pairing" | "settings"

const JUMPS: { key: JumpKey; label: string }[] = [
  { key: "vault", label: "Vault" },
  { key: "inbox", label: "Inbox" },
  { key: "conversation", label: "Conversation" },
  { key: "pairing", label: "Pairing" },
  { key: "settings", label: "Settings" },
]

function PrototypeShowcase({ onApp }: { onApp: () => void }) {
  const tab = useWaultStore((s) => s.tab)
  const overlay = useWaultStore((s) => s.overlay)
  const theme = useWaultStore((s) => s.theme)
  const view = useWaultStore((s) => s.view)
  const setTab = useWaultStore((s) => s.setTab)
  const setView = useWaultStore((s) => s.setView)
  const openConversation = useWaultStore((s) => s.openConversation)
  const openPairing = useWaultStore((s) => s.openPairing)
  const toggleTheme = useWaultStore((s) => s.toggleTheme)
  const reset = useWaultStore((s) => s.reset)

  const jump = (key: JumpKey) => {
    if (key === "vault") setTab("vault")
    else if (key === "inbox") setTab("inbox")
    else if (key === "conversation") openConversation("c1")
    else if (key === "pairing") openPairing()
    else setTab("settings")
  }

  const isActive = (key: JumpKey) => {
    if (key === "vault") return tab === "vault" && !overlay
    if (key === "inbox") return tab === "inbox" && !overlay
    if (key === "settings") return tab === "settings" && !overlay
    if (key === "conversation") return overlay?.type === "conversation"
    return overlay?.type === "pairing"
  }

  return (
    <div className="flex min-h-screen flex-col bg-[#F3F3F1] text-[#17181A]">
      {/* Page chrome (always light) */}
      <header className="mx-auto w-full max-w-[1100px] px-4 pb-6 pt-10">
        <div className="flex items-center gap-2.5">
          <span
            className="grid h-8 w-8 place-items-center rounded-xl"
            style={{ backgroundColor: "rgba(16,185,129,0.14)" }}
          >
            <ShieldCheck size={20} aria-hidden className="text-[#059669]" />
          </span>
          <span className="text-[14px] font-semibold tracking-[0.2em]">WAULT</span>
        </div>
        <h1 className="mt-4 text-[22px] font-semibold tracking-tight">Android + Web UI Concept</h1>
        <p className="mt-1 text-[13px] text-gray-500">
          One design language · Material 3 × Apple-grade restraint · v2
        </p>

        {/* Controls */}
        <div className="mt-5 flex flex-wrap items-center gap-2">
          {JUMPS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => jump(item.key)}
              aria-pressed={isActive(item.key)}
              className={cn(
                "h-8 rounded-full px-3 text-[12px] font-medium transition-colors",
                "border focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
                isActive(item.key)
                  ? "border-transparent bg-[#17181A] text-white"
                  : "border-black/[0.08] bg-white text-[#17181A] hover:bg-black/[0.03]",
              )}
            >
              {item.label}
            </button>
          ))}
          <ViewToggle view={view} onChange={setView} />
          <ModeSwitch mode="prototype" onChange={(m) => m === "app" && onApp()} />
          <span className="flex-1" />
          <button
            type="button"
            onClick={toggleTheme}
            aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
            className={cn(
              "grid h-9 w-9 place-items-center rounded-full border border-black/[0.08] bg-white text-[#17181A] transition-colors",
              "hover:bg-black/[0.03] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
            )}
          >
            {theme === "dark" ? <Sun size={16} aria-hidden /> : <Moon size={16} aria-hidden />}
          </button>
          <button
            type="button"
            onClick={reset}
            aria-label="Reset flow"
            className={cn(
              "inline-flex h-9 items-center gap-1.5 rounded-full border border-black/[0.08] bg-white px-3 text-[12px] font-medium text-[#17181A] transition-colors",
              "hover:bg-black/[0.03] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
            )}
          >
            <RotateCcw size={14} aria-hidden />
            Reset flow
          </button>
        </div>
        <p className="mt-2 text-[12px] text-gray-500">
          State (theme · view · filter) persists across reloads — flip the theme and refresh.
        </p>
      </header>

      {/* Shell — phone or web edition, swapped with a short crossfade */}
      <main className="mx-auto w-full max-w-[1100px] px-4">
        <AnimatePresence mode="wait" initial={false}>
          <motion.div
            key={view}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
          >
            {view === "phone" ? <WaultApp /> : <WebShell />}
          </motion.div>
        </AnimatePresence>
        <DesignSystemStrip />
      </main>

      <footer className="mt-auto py-6 text-center text-[12px] text-gray-500">
        Wault · Phase 0 design contract — the live app runs on the Wault Bridge (mock-free mode)
      </footer>
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* View toggle — segmented Phone / Web control                         */
/* ------------------------------------------------------------------ */

function ViewToggle({
  view,
  onChange,
}: {
  view: WaultView
  onChange: (view: WaultView) => void
}) {
  const options: { id: WaultView; label: string; icon: typeof Smartphone }[] = [
    { id: "phone", label: "Phone", icon: Smartphone },
    { id: "web", label: "Web", icon: Monitor },
  ]
  return (
    <div
      role="group"
      aria-label="Shell view"
      className="flex h-8 items-center gap-0.5 rounded-full border border-black/[0.08] bg-white p-0.5"
    >
      {options.map((option) => {
        const Icon = option.icon
        const active = view === option.id
        return (
          <button
            key={option.id}
            type="button"
            onClick={() => onChange(option.id)}
            aria-pressed={active}
            className={cn(
              "flex h-7 items-center gap-1.5 rounded-full px-2.5 text-[12px] font-medium transition-colors",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669]",
              active ? "bg-[#17181A] text-white" : "text-[#6B6F76] hover:text-[#17181A]",
            )}
          >
            <Icon size={14} aria-hidden />
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
