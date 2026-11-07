"use client"

import * as React from "react"
import { AnimatePresence, motion } from "framer-motion"
import { Check, Link2, X } from "lucide-react"

import { cn } from "@/lib/utils"
import { ACCENTS, BRAND, QR_MATRIX, withAlpha } from "../data"
import {
  FOCUS_RING,
  PHONE_BG,
  SURFACE,
  SURFACE_2,
  TonalButton,
  TEXT_2,
  TEXT_PRIMARY,
  usePhoneDark,
} from "../ui-bits"

export interface PairingResult {
  name: string
  accent: string
}

export function PairingFlow({
  onClose,
  onComplete,
}: {
  onClose: () => void
  onComplete: (result: PairingResult) => void
}) {
  const dark = usePhoneDark()
  const brand = dark ? BRAND.dark : BRAND.light

  const [step, setStep] = React.useState<1 | 2 | 3>(1)
  const [name, setName] = React.useState("")
  const [accentId, setAccentId] = React.useState("emerald")
  const accent = ACCENTS.find((a) => a.id === accentId) ?? ACCENTS[0]

  const stepVariants = {
    initial: { opacity: 0, y: 16 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -16 },
  }

  return (
    <motion.div
      initial={{ y: "100%" }}
      animate={{ y: 0 }}
      exit={{ y: "100%" }}
      transition={{ type: "spring", damping: 30, stiffness: 300 }}
      className={cn("absolute inset-0 z-30 flex flex-col", PHONE_BG)}
    >
      {/* Header */}
      <header className="flex h-14 shrink-0 items-center px-2">
        <button
          type="button"
          onClick={onClose}
          aria-label="Close pairing"
          className={cn(
            "grid h-10 w-10 shrink-0 place-items-center rounded-full transition-colors",
            "hover:bg-black/[0.05] dark:hover:bg-white/[0.08]",
            FOCUS_RING,
            TEXT_PRIMARY,
          )}
        >
          <X size={20} aria-hidden />
        </button>
        <p className={cn("min-w-0 flex-1 text-center text-[15px] font-semibold tracking-tight", TEXT_PRIMARY)}>
          Pair new account
        </p>
        <span className="h-10 w-10 shrink-0" aria-hidden />
      </header>

      {/* Step indicator */}
      <div className="flex shrink-0 items-center justify-center gap-2 pb-3 pt-1" aria-hidden>
        {[1, 2, 3].map((s) => (
          <motion.span
            key={s}
            className="h-2 w-2 rounded-full"
            animate={{
              backgroundColor:
                s < step
                  ? withAlpha(brand, 0.4)
                  : s === step
                    ? brand
                    : dark
                      ? "rgba(255,255,255,0.14)"
                      : "rgba(0,0,0,0.10)",
            }}
            transition={{ duration: 0.25 }}
          />
        ))}
      </div>

      <div className="no-scrollbar flex-1 overflow-y-auto px-5 pb-6">
        <AnimatePresence mode="wait">
          {step === 1 ? (
            <motion.div key="step-1" {...stepVariants} transition={{ duration: 0.22, ease: "easeOut" }}>
              <StepHeading>Name this workspace</StepHeading>

              <div className={cn("mt-4 flex gap-2.5 rounded-2xl p-3.5", SURFACE_2)}>
                <Link2 size={16} aria-hidden className={cn("mt-0.5 shrink-0", TEXT_2)} />
                <p className={cn("text-[13px] leading-relaxed", TEXT_PRIMARY)}>
                  Wault links this number as a companion device, exactly like WhatsApp Web. Each
                  number allows up to 4 linked devices.
                </p>
              </div>

              <label
                htmlFor="wault-account-name"
                className={cn("mt-5 block text-[12px] font-medium", TEXT_2)}
              >
                Account name
              </label>
              <input
                id="wault-account-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Marketing · Ravi"
                className={cn(
                  "mt-1.5 h-11 w-full rounded-2xl px-4 text-[14px] outline-none transition-shadow",
                  SURFACE_2,
                  TEXT_PRIMARY,
                  "placeholder:text-[#6B6F76] dark:placeholder:text-[#9AA0A6]",
                  "focus:ring-2 focus:ring-[#059669]/50 dark:focus:ring-[#34D399]/50",
                )}
              />

              <p className={cn("mt-5 block text-[12px] font-medium", TEXT_2)}>Accent</p>
              <div className="mt-2 flex items-center gap-3">
                {ACCENTS.map((option) => {
                  const selected = option.id === accentId
                  return (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => setAccentId(option.id)}
                      aria-label={`Use ${option.label} accent`}
                      aria-pressed={selected}
                      className={cn("h-6 w-6 rounded-full transition-shadow", FOCUS_RING)}
                      style={{
                        backgroundColor: option.hex,
                        boxShadow: selected
                          ? `0 0 0 2px var(--wa-bg), 0 0 0 4px ${option.hex}`
                          : undefined,
                      }}
                    />
                  )
                })}
              </div>

              <div className="mt-7">
                <TonalButton
                  variant="primary"
                  disabled={!name.trim()}
                  onClick={() => setStep(2)}
                  className="w-full"
                >
                  Continue
                </TonalButton>
              </div>
            </motion.div>
          ) : null}

          {step === 2 ? (
            <motion.div key="step-2" {...stepVariants} transition={{ duration: 0.22, ease: "easeOut" }}>
              <StepHeading>Scan the QR code</StepHeading>

              <div className="mt-6 flex justify-center">
                <div className="relative">
                  {/* Corner brackets */}
                  <CornerBracket className="-left-2.5 -top-2.5 border-l-2 border-t-2" brand={brand} />
                  <CornerBracket className="-right-2.5 -top-2.5 border-r-2 border-t-2" brand={brand} />
                  <CornerBracket className="-bottom-2.5 -left-2.5 border-b-2 border-l-2" brand={brand} />
                  <CornerBracket className="-bottom-2.5 -right-2.5 border-b-2 border-r-2" brand={brand} />

                  <div className={cn("rounded-3xl p-6 shadow-sm", SURFACE)}>
                    <div className="rounded-2xl bg-white p-3">
                      <svg
                        width={184}
                        height={184}
                        viewBox="0 0 21 21"
                        shapeRendering="crispEdges"
                        role="img"
                        aria-label="Pairing QR code"
                      >
                        {QR_MATRIX.map((row, r) =>
                          row.map((on, c) =>
                            on ? (
                              <rect key={`${r}-${c}`} x={c} y={r} width={1} height={1} fill="#17181A" />
                            ) : null,
                          ),
                        )}
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <p className={cn("mt-5 text-center text-[12px] leading-relaxed", TEXT_2)}>
                On the phone, open WhatsApp → Settings → Linked devices → Link a device
              </p>

              <div className="mt-4 flex items-center justify-center gap-2">
                <motion.span
                  className="h-2 w-2 rounded-full"
                  style={{ backgroundColor: brand }}
                  animate={{ opacity: [1, 0.3, 1] }}
                  transition={{ duration: 1.4, repeat: Infinity, ease: "easeInOut" }}
                  aria-hidden
                />
                <span className={cn("text-[12px] font-medium", TEXT_PRIMARY)}>Waiting for scan…</span>
                <span className={cn("text-[12px]", TEXT_2)}>Code refreshes in 45 s</span>
              </div>

              <div className="mt-7 flex gap-3">
                <TonalButton variant="ghost" onClick={onClose} className="flex-1">
                  Cancel
                </TonalButton>
                <TonalButton variant="primary" onClick={() => setStep(3)} className="flex-1">
                  Simulate scan
                </TonalButton>
              </div>
            </motion.div>
          ) : null}

          {step === 3 ? (
            <motion.div
              key="step-3"
              {...stepVariants}
              transition={{ duration: 0.22, ease: "easeOut" }}
              className="flex flex-col items-center pt-8 text-center"
            >
              <motion.div
                initial={{ scale: 0.4, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ type: "spring", damping: 14, stiffness: 220 }}
                className="grid h-20 w-20 place-items-center rounded-full"
                style={{ backgroundColor: withAlpha(brand, 0.14) }}
              >
                <motion.div
                  initial={{ scale: 0.6 }}
                  animate={{ scale: 1 }}
                  transition={{ delay: 0.12, type: "spring", damping: 12, stiffness: 260 }}
                  className={cn(
                    "grid h-14 w-14 place-items-center rounded-full",
                    "bg-[#059669] dark:bg-[#34D399]",
                  )}
                >
                  <Check size={28} strokeWidth={3} aria-hidden className="text-white dark:text-[#0C0D0F]" />
                </motion.div>
              </motion.div>

              <h2 className={cn("mt-5 text-[22px] font-semibold tracking-tight", TEXT_PRIMARY)}>
                Workspace linked
              </h2>
              <p className={cn("mt-1 max-w-[260px] text-[13px] leading-relaxed", TEXT_2)}>
                {name.trim() || "New workspace"} now syncs in the background
              </p>

              <div className="mt-6 w-full">
                <div className={cn("h-1.5 overflow-hidden rounded-full", SURFACE_2)}>
                  <motion.div
                    className="h-full rounded-full bg-[#059669] dark:bg-[#34D399]"
                    initial={{ width: "0%" }}
                    animate={{ width: "100%" }}
                    transition={{ duration: 2.2, ease: "easeInOut" }}
                  />
                </div>
                <p className={cn("mt-2 text-[11px]", TEXT_2)}>Syncing 214 chats…</p>
              </div>

              <div className="mt-7 w-full">
                <TonalButton
                  variant="primary"
                  className="w-full"
                  onClick={() => onComplete({ name: name.trim() || "New workspace", accent: accent.hex })}
                >
                  Go to Vault
                </TonalButton>
              </div>
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </motion.div>
  )
}

function StepHeading({ children }: { children: React.ReactNode }) {
  return (
    <h2 className={cn("text-[20px] font-semibold tracking-tight", TEXT_PRIMARY)}>{children}</h2>
  )
}

function CornerBracket({ className, brand }: { className?: string; brand: string }) {
  return (
    <motion.span
      aria-hidden
      className={cn("absolute h-7 w-7", className)}
      style={{ borderColor: brand }}
      animate={{ opacity: [1, 0.35, 1], scale: [1, 0.94, 1] }}
      transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
    />
  )
}
