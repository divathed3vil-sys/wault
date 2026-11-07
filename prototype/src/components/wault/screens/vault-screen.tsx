"use client"

import { motion } from "framer-motion"
import { ArrowDownToLine, ArrowUpFromLine, Clock, Info, Lock, Plus } from "lucide-react"

import { cn } from "@/lib/utils"
import { DEVICE_SLOTS, withAlpha, type Account } from "../data"
import {
  Avatar,
  FOCUS_RING,
  HAIRLINE,
  HOVER_TINT,
  ScreenTopBar,
  StatusPill,
  SURFACE,
  SURFACE_2,
  TEXT_2,
  TEXT_PRIMARY,
  UnreadBadge,
  usePhoneDark,
} from "../ui-bits"

export function VaultScreen({
  accounts,
  topRight,
  onOpenAccount,
  onPair,
}: {
  accounts: Account[]
  topRight?: React.ReactNode
  onOpenAccount: (accountId: string) => void
  onPair: () => void
}) {
  const dark = usePhoneDark()
  const slotsUsed = accounts.length
  const slotsFull = slotsUsed >= DEVICE_SLOTS
  const reconnecting = accounts.filter((a) => a.status === "reconnecting").length
  const allHealthy = reconnecting === 0

  return (
    <div className="flex h-full flex-col">
      <ScreenTopBar title="Vault" right={topRight} />
      <div className="no-scrollbar flex-1 overflow-y-auto px-4 pb-28">
        {/* Greeting */}
        <div className="pb-4 pt-1">
          <h2 className={cn("text-[26px] font-semibold leading-tight tracking-tight", TEXT_PRIMARY)}>
            Welcome back
          </h2>
          <p className={cn("mt-0.5 text-[13px]", TEXT_2)}>
            {accounts.length} workspaces ·{" "}
            {allHealthy ? "all healthy" : `${reconnecting} needs attention`}
          </p>
        </div>

        {/* Session health card */}
        <section className={cn("rounded-3xl p-5", SURFACE)}>
          <div className="flex items-center gap-2.5">
            <motion.span
              className="h-2.5 w-2.5 rounded-full"
              style={{ backgroundColor: allHealthy ? "#10B981" : "#F59E0B" }}
              animate={{ opacity: [1, 0.35, 1] }}
              transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
              aria-hidden
            />
            <p className={cn("text-[14px] font-medium", TEXT_PRIMARY)}>
              {allHealthy
                ? "All sessions healthy"
                : `${reconnecting} session${reconnecting === 1 ? "" : "s"} reconnecting`}
            </p>
          </div>

          <div className="mt-4">
            <div className="flex items-baseline justify-between">
              <span className={cn("text-[11px] font-medium uppercase tracking-wide", TEXT_2)}>
                Linked device slots
              </span>
              <span className={cn("text-[11px] font-semibold", TEXT_PRIMARY)}>
                {slotsUsed} of {DEVICE_SLOTS}
              </span>
            </div>
            <div className={cn("mt-2 h-1.5 overflow-hidden rounded-full", SURFACE_2)}>
              <motion.div
                className="h-full rounded-full bg-[#059669] dark:bg-[#34D399]"
                initial={false}
                animate={{ width: `${(slotsUsed / DEVICE_SLOTS) * 100}%` }}
                transition={{ type: "spring", damping: 30, stiffness: 200 }}
              />
            </div>
          </div>

          <div className={cn("mt-3.5 flex items-center gap-1.5 text-[11px]", TEXT_2)}>
            <Lock size={12} aria-hidden />
            Keys sealed with device keystore
          </div>
        </section>

        {/* Account cards */}
        <div className="mt-3 flex flex-col gap-3">
          {accounts.map((account) => (
            <button
              key={account.id}
              type="button"
              onClick={() => onOpenAccount(account.id)}
              aria-label={`Open inbox for ${account.name}`}
              className={cn(
                "w-full rounded-3xl p-4 text-left transition-colors",
                SURFACE,
                HOVER_TINT,
                FOCUS_RING,
              )}
            >
              <div className="flex items-start gap-3">
                <Avatar name={account.name} accent={account.accent} size={44} ringPx={1.5} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className={cn("truncate text-[15px] font-medium leading-tight", TEXT_PRIMARY)}>
                      {account.name}
                    </p>
                    <StatusPill status={account.status} />
                  </div>
                  <p className={cn("mt-0.5 truncate text-[12px]", TEXT_2)}>{account.phone}</p>
                </div>
              </div>
              <div className="mt-3 flex items-center justify-between">
                <div className={cn("flex items-center gap-4 text-[11px]", TEXT_2)}>
                  <span className="flex items-center gap-1">
                    <ArrowDownToLine size={12} aria-hidden />
                    {account.receivedToday} in
                  </span>
                  <span className="flex items-center gap-1">
                    <ArrowUpFromLine size={12} aria-hidden />
                    {account.sentToday} out
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock size={12} aria-hidden />
                    {account.lastActive}
                  </span>
                </div>
                <UnreadBadge count={account.unread} accent={account.accent} />
              </div>
            </button>
          ))}

          {/* Pair new account / slots full */}
          {slotsFull ? (
            <div className={cn("rounded-3xl border border-dashed p-4", SURFACE, HAIRLINE)}>
              <div className="flex items-center gap-3">
                <span
                  className="grid h-10 w-10 shrink-0 place-items-center rounded-full"
                  style={{ backgroundColor: withAlpha("#F59E0B", dark ? 0.16 : 0.12) }}
                >
                  <Info size={18} aria-hidden className="text-[#B45309] dark:text-[#FBBF24]" />
                </span>
                <div className="min-w-0">
                  <p className={cn("text-[14px] font-medium leading-tight", TEXT_PRIMARY)}>
                    Device slots full (4 of 4)
                  </p>
                  <p className={cn("mt-0.5 text-[12px]", TEXT_2)}>
                    Unlink a workspace to add another number
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={onPair}
              aria-label="Pair new account"
              className={cn(
                "w-full rounded-3xl border border-dashed border-black/20 p-4 text-left transition-colors dark:border-white/20",
                "hover:bg-black/[0.02] dark:hover:bg-white/[0.04]",
                FOCUS_RING,
              )}
            >
              <div className="flex items-center gap-3">
                <span className={cn("grid h-10 w-10 shrink-0 place-items-center rounded-full", SURFACE_2)}>
                  <Plus size={18} aria-hidden className={TEXT_PRIMARY} />
                </span>
                <div className="min-w-0">
                  <p className={cn("text-[14px] font-medium leading-tight", TEXT_PRIMARY)}>
                    Pair new account
                  </p>
                  <p className={cn("mt-0.5 text-[12px]", TEXT_2)}>
                    Links as a companion device, like WhatsApp Web
                  </p>
                </div>
              </div>
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
