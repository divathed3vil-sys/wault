"use client"

import { MessageCircle, Settings2, ShieldCheck } from "lucide-react"
import { cn } from "@/lib/utils"
import { FOCUS_RING, HAIRLINE, SURFACE_2, TEXT_2, TEXT_PRIMARY } from "./ui-bits"
import type { WaultTab } from "./wault-app"

const ITEMS: { id: WaultTab; label: string; icon: typeof ShieldCheck }[] = [
  { id: "vault", label: "Vault", icon: ShieldCheck },
  { id: "inbox", label: "Inbox", icon: MessageCircle },
  { id: "settings", label: "Settings", icon: Settings2 },
]

export function BottomNav({
  tab,
  onTab,
  unreadTotal,
}: {
  tab: WaultTab
  onTab: (tab: WaultTab) => void
  unreadTotal: number
}) {
  return (
    <nav
      aria-label="Primary"
      className={cn(
        "absolute inset-x-0 bottom-0 z-20 border-t bg-white/95 backdrop-blur-sm dark:bg-[#16181B]/95",
        HAIRLINE,
      )}
    >
      <div className="flex h-16 items-start justify-around px-2 pt-1.5">
        {ITEMS.map((item) => {
          const active = tab === item.id
          const Icon = item.icon
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onTab(item.id)}
              aria-label={item.label}
              aria-current={active ? "page" : undefined}
              className={cn(
                "flex w-20 flex-col items-center gap-1 rounded-2xl pt-1 focus-visible:outline-none",
                FOCUS_RING,
              )}
            >
              <span
                className={cn(
                  "relative flex items-center rounded-full px-4 py-1.5 transition-colors duration-200",
                  active ? SURFACE_2 : "bg-transparent",
                )}
              >
                <Icon
                  size={20}
                  aria-hidden
                  className={cn("transition-colors duration-200", active ? TEXT_PRIMARY : TEXT_2)}
                  strokeWidth={active ? 2.25 : 2}
                />
                {item.id === "inbox" && unreadTotal > 0 ? (
                  <span className="absolute -right-1.5 -top-1 grid h-4 min-w-4 place-items-center rounded-full bg-[#059669] px-1 text-[9px] font-bold text-white dark:bg-[#34D399] dark:text-[#0C0D0F]">
                    {unreadTotal}
                  </span>
                ) : null}
              </span>
              <span
                className={cn(
                  "text-[11px] leading-none transition-colors duration-200",
                  active ? cn("font-medium", TEXT_PRIMARY) : TEXT_2,
                )}
              >
                {item.label}
              </span>
            </button>
          )
        })}
      </div>
    </nav>
  )
}
