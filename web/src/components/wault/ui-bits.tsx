"use client"

import * as React from "react"
import { motion } from "framer-motion"
import { ChevronRight } from "lucide-react"
import { Drawer as VaulDrawer } from "vaul"

import { cn } from "@/lib/utils"
import { initialsOf, withAlpha, type Account, type AccountStatus } from "./data"

/* ------------------------------------------------------------------ */
/* Theme helpers — Tailwind classes scoped by the .dark phone wrapper. */
/* ------------------------------------------------------------------ */

export const PHONE_BG = "bg-[#F7F7F5] dark:bg-[#0C0D0F]"
export const SURFACE = "bg-white dark:bg-[#16181B]"
export const SURFACE_2 = "bg-[#F0F0EE] dark:bg-[#1E2126]"
export const TEXT_PRIMARY = "text-[#17181A] dark:text-[#F2F3F4]"
export const TEXT_2 = "text-[#6B6F76] dark:text-[#9AA0A6]"
export const HAIRLINE = "border-black/[0.07] dark:border-white/[0.08]"
export const HAIRLINE_DIVIDE = "divide-black/[0.07] dark:divide-white/[0.08]"
export const BRAND_BG = "bg-[#059669] dark:bg-[#34D399]"
export const BRAND_TEXT = "text-[#059669] dark:text-[#34D399]"
export const HOVER_TINT = "hover:bg-black/[0.03] dark:hover:bg-white/[0.05]"
export const FOCUS_RING =
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669] dark:focus-visible:ring-[#34D399]"

/* ------------------------------------------------------------------ */
/* Phone dark-mode context (for inline colors that need theme awareness)*/
/* ------------------------------------------------------------------ */

const PhoneDarkContext = React.createContext(false)

export function PhoneThemeProvider({ dark, children }: { dark: boolean; children: React.ReactNode }) {
  return <PhoneDarkContext.Provider value={dark}>{children}</PhoneDarkContext.Provider>
}

export function usePhoneDark(): boolean {
  return React.useContext(PhoneDarkContext)
}

/* ------------------------------------------------------------------ */
/* Avatar — initials on accent-tinted disc with an accent ring.        */
/* ------------------------------------------------------------------ */

export function Avatar({
  name,
  accent,
  size = 44,
  ringPx = 1.5,
  className,
}: {
  name: string
  accent: string
  size?: number
  ringPx?: number
  className?: string
}) {
  return (
    <span
      aria-hidden
      className={cn(
        "grid shrink-0 select-none place-items-center rounded-full font-semibold",
        className,
      )}
      style={{
        width: size,
        height: size,
        fontSize: Math.round(size * 0.32),
        letterSpacing: "0.02em",
        backgroundColor: withAlpha(accent, 0.14),
        color: accent,
        boxShadow: `0 0 0 ${ringPx}px ${accent}`,
      }}
    >
      {initialsOf(name)}
    </span>
  )
}

/* ------------------------------------------------------------------ */
/* StatusPill — connected / reconnecting tint pills with dots.         */
/* ------------------------------------------------------------------ */

const STATUS_COLORS: Record<
  AccountStatus,
  { dot: string; textLight: string; textDark: string }
> = {
  connected: { dot: "#10B981", textLight: "#047857", textDark: "#34D399" },
  reconnecting: { dot: "#F59E0B", textLight: "#B45309", textDark: "#FBBF24" },
  disconnected: { dot: "#9AA0A6", textLight: "#6B6F76", textDark: "#9AA0A6" },
}

export function StatusPill({ status, label }: { status: AccountStatus; label?: string }) {
  const dark = usePhoneDark()
  const colors = STATUS_COLORS[status]
  return (
    <span
      className="inline-flex shrink-0 items-center gap-1.5 rounded-full px-2 py-0.5 text-[11px] font-medium"
      style={{
        backgroundColor: withAlpha(colors.dot, 0.12),
        color: dark ? colors.textDark : colors.textLight,
      }}
    >
      <motion.span
        className="h-1.5 w-1.5 rounded-full"
        style={{ backgroundColor: colors.dot }}
        animate={status === "reconnecting" ? { opacity: [1, 0.3, 1] } : { opacity: 1 }}
        transition={status === "reconnecting" ? { duration: 1.4, repeat: Infinity, ease: "easeInOut" } : undefined}
      />
      {label ??
        (status === "connected"
          ? "Connected"
          : status === "reconnecting"
            ? "Reconnecting"
            : "Offline")}
    </span>
  )
}

/* ------------------------------------------------------------------ */
/* StatusDot — small presence dot for sheet rows.                      */
/* ------------------------------------------------------------------ */

export function StatusDot({ account }: { account: Account }) {
  const color =
    account.status === "connected"
      ? "#10B981"
      : account.status === "reconnecting"
        ? "#F59E0B"
        : "#9AA0A6"
  return (
    <motion.span
      className="inline-block h-2 w-2 shrink-0 rounded-full"
      style={{ backgroundColor: color }}
      animate={account.status === "reconnecting" ? { opacity: [1, 0.3, 1] } : { opacity: 1 }}
      transition={
        account.status === "reconnecting"
          ? { duration: 1.4, repeat: Infinity, ease: "easeInOut" }
          : undefined
      }
      aria-hidden
    />
  )
}

/* ------------------------------------------------------------------ */
/* UnreadBadge — accent disc with white count.                         */
/* ------------------------------------------------------------------ */

export function UnreadBadge({ count, accent }: { count: number; accent: string }) {
  if (count <= 0) return null
  return (
    <span
      className="grid h-5 min-w-5 shrink-0 place-items-center rounded-full px-1.5 text-[11px] font-semibold text-white"
      style={{ backgroundColor: accent }}
    >
      {count}
    </span>
  )
}

/* ------------------------------------------------------------------ */
/* SectionRow — settings / grouped list row.                           */
/* ------------------------------------------------------------------ */

export function SectionRow({
  icon,
  title,
  subtitle,
  value,
  right,
  onClick,
  ariaLabel,
}: {
  icon?: React.ReactNode
  title: string
  subtitle?: string
  value?: string
  right?: React.ReactNode
  onClick?: () => void
  ariaLabel?: string
}) {
  const content = (
    <>
      {icon ? <span className="grid w-5 shrink-0 place-items-center">{icon}</span> : null}
      <span className="min-w-0 flex-1">
        <span className={cn("block truncate text-[13.5px] font-medium leading-tight", TEXT_PRIMARY)}>{title}</span>
        {subtitle ? (
          <span className={cn("mt-0.5 block truncate text-[11.5px] leading-tight", TEXT_2)}>{subtitle}</span>
        ) : null}
      </span>
      {value ? (
        <span className={cn("shrink-0 text-[12.5px]", TEXT_2)}>{value}</span>
      ) : null}
      {right}
      {onClick ? <ChevronRight size={16} className={cn("shrink-0", TEXT_2)} aria-hidden /> : null}
    </>
  )
  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        aria-label={ariaLabel}
        className={cn(
          "flex h-12 w-full items-center gap-3 px-4 text-left transition-colors first:rounded-t-3xl last:rounded-b-3xl",
          HOVER_TINT,
          FOCUS_RING,
        )}
      >
        {content}
      </button>
    )
  }
  return <div className="flex h-12 w-full items-center gap-3 px-4">{content}</div>
}

/* ------------------------------------------------------------------ */
/* TonalButton — pill button in tonal / primary / ghost variants.      */
/* ------------------------------------------------------------------ */

export function TonalButton({
  children,
  variant = "tonal",
  onClick,
  disabled,
  className,
  ariaLabel,
}: {
  children: React.ReactNode
  variant?: "tonal" | "primary" | "ghost"
  onClick?: () => void
  disabled?: boolean
  className?: string
  ariaLabel?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      className={cn(
        "inline-flex h-10 items-center justify-center gap-2 rounded-full px-4 text-[13px] font-medium transition-all",
        "disabled:cursor-not-allowed disabled:opacity-40",
        FOCUS_RING,
        variant === "primary" && cn(BRAND_BG, "text-white shadow-sm"),
        variant === "tonal" && cn(SURFACE_2, TEXT_PRIMARY),
        variant === "ghost" && cn(TEXT_2, HOVER_TINT),
        className,
      )}
    >
      {children}
    </button>
  )
}

/* ------------------------------------------------------------------ */
/* ScreenTopBar — per-screen large-ish title bar with right cluster.   */
/* ------------------------------------------------------------------ */

export function ScreenTopBar({ title, right }: { title: string; right?: React.ReactNode }) {
  return (
    <div className="flex h-14 shrink-0 items-center justify-between px-4">
      <h1 className={cn("text-[17px] font-semibold tracking-tight", TEXT_PRIMARY)}>{title}</h1>
      {right}
    </div>
  )
}

/* ------------------------------------------------------------------ */
/* AvatarCluster — stacked avatars acting as the switcher trigger.     */
/* ------------------------------------------------------------------ */

export function AvatarCluster({ accounts, onOpen }: { accounts: Account[]; onOpen: () => void }) {
  const shown = accounts.slice(0, 2)
  const rest = Math.max(0, accounts.length - 2)
  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label="Switch account"
      className={cn("flex -space-x-3 rounded-full focus-visible:outline-none", FOCUS_RING)}
    >
      {shown.map((account) => (
        <span
          key={account.id}
          className="rounded-full p-[2px]"
          style={{ backgroundColor: "var(--wa-bg)" }}
        >
          <Avatar name={account.name} accent={account.accent} size={28} ringPx={1.5} />
        </span>
      ))}
      {rest > 0 ? (
        <span
          className={cn(
            "grid h-8 w-8 place-items-center rounded-full border text-[10px] font-semibold",
            SURFACE_2,
            TEXT_2,
            HAIRLINE,
          )}
        >
          +{rest}
        </span>
      ) : null}
    </button>
  )
}

/* ------------------------------------------------------------------ */
/* WaultSheet — vaul bottom sheet constrained to the phone frame.      */
/* The portal container is an element inside the transformed phone     */
/* wrapper, so "fixed" positioning resolves to the phone itself.       */
/* ------------------------------------------------------------------ */

export function WaultSheet({
  open,
  onOpenChange,
  host,
  title,
  description,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  host: HTMLElement | null
  title: string
  description?: string
  children: React.ReactNode
}) {
  return (
    <VaulDrawer.Root open={open} onOpenChange={onOpenChange} repositionInputs={false}>
      <VaulDrawer.Portal container={host}>
        <VaulDrawer.Overlay className="fixed inset-0 z-40 bg-black/35" />
        <VaulDrawer.Content
          aria-describedby={undefined}
          className={cn(
            "fixed inset-x-0 bottom-0 z-50 flex flex-col rounded-t-[28px] pb-7 outline-none",
            SURFACE,
          )}
        >
          <VaulDrawer.Title className="sr-only">{title}</VaulDrawer.Title>
          <div className="mx-auto mt-2 h-1 w-9 shrink-0 rounded-full bg-black/15 dark:bg-white/20" />
          <div className="px-5 pb-1 pt-3">
            <p className={cn("text-[15px] font-semibold tracking-tight", TEXT_PRIMARY)}>{title}</p>
            {description ? <p className={cn("mt-0.5 text-[12px]", TEXT_2)}>{description}</p> : null}
          </div>
          <div className="px-5 pt-2">{children}</div>
        </VaulDrawer.Content>
      </VaulDrawer.Portal>
    </VaulDrawer.Root>
  )
}

/* ------------------------------------------------------------------ */
/* SheetRow — generic row used inside sheets.                          */
/* ------------------------------------------------------------------ */

export function SheetRow({
  leading,
  title,
  subtitle,
  right,
  onClick,
}: {
  leading: React.ReactNode
  title: string
  subtitle?: string
  right?: React.ReactNode
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-3 rounded-2xl px-2 py-2.5 text-left transition-colors",
        HOVER_TINT,
        FOCUS_RING,
      )}
    >
      {leading}
      <span className="min-w-0 flex-1">
        <span className={cn("block truncate text-[14px] font-medium leading-tight", TEXT_PRIMARY)}>{title}</span>
        {subtitle ? <span className={cn("mt-0.5 block truncate text-[12px]", TEXT_2)}>{subtitle}</span> : null}
      </span>
      {right}
      {onClick ? <ChevronRight size={16} className={cn("shrink-0", TEXT_2)} aria-hidden /> : null}
    </button>
  )
}
