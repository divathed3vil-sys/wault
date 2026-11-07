"use client"

import * as React from "react"
import { toast } from "sonner"
import { Layers, Plus, Send } from "lucide-react"

import { cn } from "@/lib/utils"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { accountShortName, withAlpha, type Account } from "./data"
import {
  Avatar,
  FOCUS_RING,
  HAIRLINE,
  SheetRow,
  StatusDot,
  SURFACE_2,
  TonalButton,
  TEXT_2,
  TEXT_PRIMARY,
  WaultSheet,
} from "./ui-bits"

/* ------------------------------------------------------------------ */
/* Account switcher sheet                                              */
/* ------------------------------------------------------------------ */

export function AccountSwitcherSheet({
  open,
  onOpenChange,
  host,
  accounts,
  onPickAll,
  onPick,
  onPair,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  host: HTMLElement | null
  accounts: Account[]
  onPickAll: () => void
  onPick: (accountId: string) => void
  onPair: () => void
}) {
  return (
    <WaultSheet
      open={open}
      onOpenChange={onOpenChange}
      host={host}
      title="Workspaces"
      description="Switch the workspace you are working in"
    >
      <div className="pb-2">
        <SheetRow
          leading={
            <span className={cn("grid h-10 w-10 shrink-0 place-items-center rounded-full", SURFACE_2)}>
              <Layers size={18} aria-hidden className={TEXT_PRIMARY} />
            </span>
          }
          title="All workspaces"
          subtitle="Every chat across accounts"
          onClick={onPickAll}
        />
        <div className={cn("my-1 border-t", HAIRLINE)} />
        {accounts.map((account) => (
          <SheetRow
            key={account.id}
            leading={<Avatar name={account.name} accent={account.accent} size={40} ringPx={1.5} />}
            title={account.name}
            subtitle={account.phone}
            right={<StatusDot account={account} />}
            onClick={() => onPick(account.id)}
          />
        ))}
        <div className={cn("my-1 border-t", HAIRLINE)} />
        <SheetRow
          leading={
            <span
              className="grid h-10 w-10 shrink-0 place-items-center rounded-full"
              style={{ backgroundColor: withAlpha("#10B981", 0.14) }}
            >
              <Plus size={18} aria-hidden className="text-[#059669] dark:text-[#34D399]" />
            </span>
          }
          title="Pair new account"
          subtitle="Links as a companion device"
          onClick={onPair}
        />
      </div>
    </WaultSheet>
  )
}

/* ------------------------------------------------------------------ */
/* New message sheet                                                   */
/* ------------------------------------------------------------------ */

export function NewMessageSheet({
  open,
  onOpenChange,
  host,
  accounts,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  host: HTMLElement | null
  accounts: Account[]
}) {
  const [accountId, setAccountId] = React.useState(accounts[0]?.id ?? "")
  const [to, setTo] = React.useState("")
  const [body, setBody] = React.useState("")

  // Keep selection valid if the account list changes (e.g. after pairing).
  React.useEffect(() => {
    if (!accounts.some((a) => a.id === accountId) && accounts[0]) {
      setAccountId(accounts[0].id)
    }
  }, [accounts, accountId])

  const canSend = Boolean(accountId) && to.trim().length > 0 && body.trim().length > 0

  const send = () => {
    if (!canSend) return
    toast("Queued — the engine connects in Phase 2")
    setTo("")
    setBody("")
    onOpenChange(false)
  }

  return (
    <WaultSheet
      open={open}
      onOpenChange={onOpenChange}
      host={host}
      title="New message"
      description="Compose from any linked workspace"
    >
      <div className="pb-2">
        <p className={cn("text-[12px] font-medium", TEXT_2)}>From</p>
        <div className="no-scrollbar mt-2 flex gap-2 overflow-x-auto pb-1">
          {accounts.map((account) => {
            const selected = account.id === accountId
            return (
              <button
                key={account.id}
                type="button"
                onClick={() => setAccountId(account.id)}
                aria-pressed={selected}
                aria-label={`Send from ${account.name}`}
                className={cn(
                  "flex shrink-0 items-center gap-2 rounded-full py-1.5 pl-1.5 pr-3 transition-all",
                  FOCUS_RING,
                )}
                style={{
                  backgroundColor: selected ? withAlpha(account.accent, 0.1) : undefined,
                  boxShadow: selected
                    ? `inset 0 0 0 1.5px ${account.accent}`
                    : `inset 0 0 0 1px var(--wa-hairline, rgba(0,0,0,0.07))`,
                }}
              >
                <Avatar name={account.name} accent={account.accent} size={26} ringPx={0} />
                <span className={cn("text-[12px] font-medium", TEXT_PRIMARY)}>
                  {accountShortName(account.name)}
                </span>
              </button>
            )
          })}
        </div>

        <label htmlFor="wault-new-to" className={cn("mt-4 block text-[12px] font-medium", TEXT_2)}>
          To
        </label>
        <Input
          id="wault-new-to"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          placeholder="Name or number"
          className={cn(
            "mt-1.5 h-11 rounded-2xl border-0 px-4 text-[14px] shadow-none",
            SURFACE_2,
            "focus-visible:ring-2 focus-visible:ring-[#059669]/50 dark:focus-visible:ring-[#34D399]/50",
          )}
        />

        <label htmlFor="wault-new-body" className={cn("mt-3 block text-[12px] font-medium", TEXT_2)}>
          Message
        </label>
        <Textarea
          id="wault-new-body"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Type your message"
          rows={3}
          className={cn(
            "mt-1.5 min-h-20 rounded-2xl border-0 px-4 py-3 text-[14px] shadow-none",
            SURFACE_2,
            "focus-visible:ring-2 focus-visible:ring-[#059669]/50 dark:focus-visible:ring-[#34D399]/50",
          )}
        />

        <div className="mt-4">
          <TonalButton
            variant="primary"
            className="w-full"
            disabled={!canSend}
            onClick={send}
          >
            <Send size={15} aria-hidden />
            Send message
          </TonalButton>
        </div>
      </div>
    </WaultSheet>
  )
}
