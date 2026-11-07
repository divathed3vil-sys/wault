"use client"

import * as React from "react"
import { toast } from "sonner"
import { Github, Lock, ShieldCheck } from "lucide-react"

import { cn } from "@/lib/utils"
import { Switch } from "@/components/ui/switch"
import { ACCENTS, BRAND, withAlpha, type Account } from "../data"
import {
  Avatar,
  FOCUS_RING,
  HAIRLINE,
  HAIRLINE_DIVIDE,
  ScreenTopBar,
  SectionRow,
  StatusDot,
  SURFACE,
  SURFACE_2,
  TonalButton,
  TEXT_2,
  TEXT_PRIMARY,
  usePhoneDark,
  WaultSheet,
} from "../ui-bits"

type ThemePref = "light" | "dark" | "system"

export function SettingsScreen({
  accounts,
  theme,
  onTheme,
  onOpenChannels,
  sheetHost,
  channelsOpen,
  onCloseChannels,
  topRight,
}: {
  accounts: Account[]
  theme: ThemePref
  onTheme: (theme: ThemePref) => void
  onOpenChannels: () => void
  sheetHost: HTMLElement | null
  channelsOpen: boolean
  onCloseChannels: () => void
  topRight?: React.ReactNode
}) {
  const dark = usePhoneDark()
  const brand = dark ? BRAND.dark : BRAND.light

  const [masterAlerts, setMasterAlerts] = React.useState(true)
  const [quietHours, setQuietHours] = React.useState(true)
  const [autoLock, setAutoLock] = React.useState(true)
  const [channelToggles, setChannelToggles] = React.useState<Record<string, boolean>>(() =>
    Object.fromEntries(accounts.map((a) => [a.id, true])),
  )

  return (
    <div className="flex h-full flex-col">
      <ScreenTopBar title="Settings" right={topRight} />
      <div className="no-scrollbar flex-1 overflow-y-auto px-4 pb-28">
        {/* Brand card */}
        <div className={cn("mt-1 flex items-center gap-3 rounded-3xl p-5", SURFACE)}>
          <span
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full"
            style={{ backgroundColor: withAlpha("#10B981", dark ? 0.18 : 0.14) }}
          >
            <ShieldCheck size={22} aria-hidden style={{ color: brand }} />
          </span>
          <div className="min-w-0 flex-1">
            <p className={cn("text-[15px] font-medium leading-tight", TEXT_PRIMARY)}>Wault v2.0</p>
            <p className={cn("mt-0.5 text-[12px]", TEXT_2)}>Design preview · Phase 0</p>
          </div>
          <span className={cn("shrink-0 rounded-full px-2.5 py-1 text-[11px] font-medium", SURFACE_2, TEXT_2)}>
            Tier · Capable
          </span>
        </div>

        {/* Appearance */}
        <SettingsSection label="Appearance">
          <div className={cn("flex h-12 items-center justify-between gap-3 px-4", HAIRLINE)}>
            <span className={cn("shrink-0 text-[13.5px] font-medium", TEXT_PRIMARY)}>Theme</span>
            <div className={cn("flex rounded-full p-0.5", SURFACE_2)}>
              {(["light", "dark", "system"] as const).map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => onTheme(option)}
                  aria-pressed={theme === option}
                  className={cn(
                    "h-7 rounded-full px-2.5 text-[11px] font-medium capitalize transition-all",
                    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#059669] dark:focus-visible:ring-[#34D399]",
                    theme === option
                      ? cn("bg-white text-[#17181A] shadow-sm dark:bg-[#2E3238] dark:text-[#F2F3F4]")
                      : TEXT_2,
                  )}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>
          <div className={cn("flex h-12 items-center justify-between gap-3 px-4", HAIRLINE)}>
            <span className={cn("shrink-0 text-[13.5px] font-medium", TEXT_PRIMARY)}>Default accent</span>
            <span className="flex items-center gap-2">
              {ACCENTS.map((option) => (
                <span
                  key={option.id}
                  className="h-4 w-4 rounded-full"
                  style={{
                    backgroundColor: option.hex,
                    boxShadow:
                      option.id === "emerald"
                        ? `0 0 0 2px var(--wa-bg), 0 0 0 3.5px ${option.hex}`
                        : undefined,
                    opacity: option.id === "emerald" ? 1 : 0.85,
                  }}
                />
              ))}
            </span>
          </div>
        </SettingsSection>

        {/* Notifications */}
        <SettingsSection label="Notifications">
          <div className={cn(FAUX_ROW)}>
            <span className={cn("text-[13.5px] font-medium", TEXT_PRIMARY)}>Master alerts</span>
            <Switch
              checked={masterAlerts}
              onCheckedChange={setMasterAlerts}
              aria-label="Master alerts"
            />
          </div>
          <div className={FAUX_ROW}>
            <span className="min-w-0">
              <span className={cn("block text-[13.5px] font-medium leading-tight", TEXT_PRIMARY)}>
                Quiet hours
              </span>
              <span className={cn("mt-0.5 block text-[11.5px] leading-tight", TEXT_2)}>
                10:00 pm – 7:00 am
              </span>
            </span>
            <Switch checked={quietHours} onCheckedChange={setQuietHours} aria-label="Quiet hours" />
          </div>
          <SectionRow
            title="Per-account channels"
            onClick={onOpenChannels}
            ariaLabel="Per-account channels"
          />
        </SettingsSection>

        {/* Sessions & security */}
        <SettingsSection label="Sessions & security">
          <div className={cn(FAUX_ROW)}>
            <span className={cn("text-[13.5px] font-medium", TEXT_PRIMARY)}>Auto-lock</span>
            <Switch checked={autoLock} onCheckedChange={setAutoLock} aria-label="Auto-lock" />
          </div>
          <SectionRow
            icon={<Lock size={15} aria-hidden className={TEXT_2} />}
            title="Encrypted vault"
            subtitle="Keystore-backed · AES-GCM"
            onClick={() => toast("Vault internals land in Phase 3")}
            ariaLabel="Encrypted vault"
          />
          <SectionRow
            title="Export encrypted backup"
            onClick={() => toast("Backup lands with the engine (Phase 3)")}
            ariaLabel="Export encrypted backup"
          />
        </SettingsSection>

        {/* About */}
        <SettingsSection label="About">
          <SectionRow title="Version" value="2.0.0-alpha" />
          <SectionRow
            icon={<Github size={15} aria-hidden className={TEXT_2} />}
            title="GitHub repository"
            onClick={() => toast("The repository goes public with Phase 2")}
            ariaLabel="GitHub repository"
          />
          <SectionRow
            title="Licenses"
            onClick={() => toast("License details land in Phase 3")}
            ariaLabel="Licenses"
          />
        </SettingsSection>
      </div>

      <ChannelsSheet
        accounts={accounts}
        open={channelsOpen}
        onOpenChange={(open) => {
          if (!open) onCloseChannels()
        }}
        host={sheetHost}
        toggles={channelToggles}
        onToggle={(id, next) => setChannelToggles((prev) => ({ ...prev, [id]: next }))}
      />
    </div>
  )
}

const FAUX_ROW = "flex h-12 w-full items-center justify-between gap-3 px-4"

function SettingsSection({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <section className="mt-5">
      <h3 className={cn("mb-1.5 px-2 text-[12px] font-medium uppercase tracking-wide", TEXT_2)}>
        {label}
      </h3>
      <div className={cn("divide-y overflow-hidden rounded-3xl", SURFACE, HAIRLINE_DIVIDE)}>
        {children}
      </div>
    </section>
  )
}

export function ChannelsSheet({
  accounts,
  open,
  onOpenChange,
  host,
  toggles,
  onToggle,
}: {
  accounts: Account[]
  open: boolean
  onOpenChange: (open: boolean) => void
  host: HTMLElement | null
  toggles: Record<string, boolean>
  onToggle: (id: string, next: boolean) => void
}) {
  return (
    <WaultSheet
      open={open}
      onOpenChange={onOpenChange}
      host={host}
      title="Per-account channels"
      description="Route notifications per workspace"
    >
      <div className="pb-2">
        <div className={HAIRLINE_DIVIDE}>{accounts.map((account) => (
          <div
            key={account.id}
            className="flex items-center gap-3 py-2.5"
          >
            <Avatar name={account.name} accent={account.accent} size={36} ringPx={1.5} />
            <div className="min-w-0 flex-1">
              <p className={cn("truncate text-[14px] font-medium leading-tight", TEXT_PRIMARY)}>
                {account.name}
              </p>
              <p className={cn("mt-0.5 flex items-center gap-1.5 text-[12px]", TEXT_2)}>
                <StatusDot account={account} />
                {account.status === "connected"
                  ? "Connected"
                  : account.status === "reconnecting"
                    ? "Reconnecting"
                    : "Offline"}
              </p>
            </div>
            <Switch
              checked={toggles[account.id] ?? true}
              onCheckedChange={(next) => onToggle(account.id, next)}
              aria-label={`Notifications for ${account.name}`}
            />
          </div>
        ))}</div>
        <div className="mt-4">
          <TonalButton variant="tonal" className="w-full" onClick={() => onOpenChange(false)}>
            Done
          </TonalButton>
        </div>
      </div>
    </WaultSheet>
  )
}
