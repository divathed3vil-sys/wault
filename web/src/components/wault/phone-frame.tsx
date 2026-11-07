"use client"

import * as React from "react"

import { cn } from "@/lib/utils"
import { TEXT_PRIMARY } from "./ui-bits"
import { BatteryFull, Signal, Wifi } from "lucide-react"

export type SheetHostSetter = (el: HTMLDivElement | null) => void

/**
 * Android phone frame — 390x844 with dark bezel, status bar, camera hole
 * and gesture bar. The phone carries the `dark` class so all internal
 * dark: variants are scoped to it; the page chrome stays light.
 */
export function PhoneFrame({
  dark,
  children,
  sheetHostRef,
}: {
  dark: boolean
  children: React.ReactNode
  sheetHostRef: SheetHostSetter
}) {
  const [scale, setScale] = React.useState(1)

  React.useEffect(() => {
    const measure = () => {
      const next = Math.min(1, (window.innerWidth - 24) / 410)
      setScale(next)
    }
    measure()
    window.addEventListener("resize", measure)
    return () => window.removeEventListener("resize", measure)
  }, [])

  return (
    <div className="relative w-full" style={{ height: 844 * scale + 40 }}>
      <div
        className="mx-auto w-[390px] shrink-0"
        style={{ transform: `scale(${scale})`, transformOrigin: "top center", height: 844 }}
      >
        <div
          className={cn(
            "relative flex h-full w-full flex-col overflow-hidden rounded-[44px] border-[10px] border-[#1A1B1E]",
            "bg-[#F7F7F5] shadow-[0_40px_80px_-20px_rgba(0,0,0,0.35)] dark:bg-[#0C0D0F]",
            dark && "dark",
          )}
          style={
            {
              "--wa-bg": dark ? "#0C0D0F" : "#F7F7F5",
              "--wa-hairline": dark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.07)",
            } as React.CSSProperties
          }
        >
          <StatusBar />
          {children}
          <GestureBar />
          {/* Portal host for vaul sheets; display:contents keeps layout intact
              while the transformed ancestor scopes "fixed" positioning. */}
          <div ref={sheetHostRef} className="contents" />
        </div>
      </div>
    </div>
  )
}

function StatusBar() {
  return (
    <div className="relative z-10 flex h-11 shrink-0 items-center justify-between px-6">
      <span className={cn("text-[13px] font-semibold tracking-wide", TEXT_PRIMARY)}>16:05</span>
      <span
        aria-hidden
        className="absolute left-1/2 top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#1A1B1E] ring-1 ring-black/20 dark:bg-[#2A2D33] dark:ring-white/15"
      />
      <span className={cn("flex items-center gap-1.5", TEXT_PRIMARY)}>
        <Signal size={16} aria-hidden />
        <Wifi size={16} aria-hidden />
        <BatteryFull size={16} aria-hidden />
      </span>
    </div>
  )
}

function GestureBar() {
  return (
    <div className="z-10 flex h-6 shrink-0 items-center justify-center">
      <div className="h-1 w-[108px] rounded-full bg-black/25 dark:bg-white/30" aria-hidden />
    </div>
  )
}
