import { cn } from "@/lib/utils"
import { ACCENTS, BRAND, DEVICE_SLOTS } from "@/components/wault/data"

/**
 * Page-level design-system strip shown below the phone. Page chrome is
 * always light — no dark: variants here.
 */

const SURFACE_PAIRS = [
  { name: "Background", light: "#F7F7F5", dark: "#0C0D0F" },
  { name: "Surface", light: "#FFFFFF", dark: "#16181B" },
  { name: "Surface 2", light: "#F0F0EE", dark: "#1E2126" },
  { name: "Text", light: "#17181A", dark: "#F2F3F4" },
]

const TYPE_RAMP = [
  { label: "Display", sample: "Display", size: 26, weight: "font-semibold tracking-tight" },
  { label: "Title", sample: "Title", size: 20, weight: "font-semibold tracking-tight" },
  { label: "Body", sample: "Body copy", size: 15, weight: "font-normal" },
  { label: "Label", sample: "LABEL", size: 12, weight: "font-medium tracking-wide" },
]

export function DesignSystemStrip() {
  return (
    <section aria-label="Design system" className="mt-14">
      <div className="flex items-baseline justify-between">
        <h2 className="text-[13px] font-semibold uppercase tracking-[0.14em] text-[#6B6F76]">
          Design system
        </h2>
        <span className="text-[12px] text-[#9AA0A6]">4 tokens · 2 themes</span>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-4">
        {/* Surfaces */}
        <div className="rounded-2xl border border-black/[0.06] bg-white p-5">
          <h3 className="text-[14px] font-semibold text-[#17181A]">Surfaces</h3>
          <div className="mt-4 flex flex-col gap-3">
            {SURFACE_PAIRS.map((pair) => (
              <div key={pair.name} className="flex items-center justify-between gap-3">
                <span className="text-[12px] font-medium text-[#17181A]">{pair.name}</span>
                <span className="flex items-center gap-2">
                  <Swatch hex={pair.light} theme="light" />
                  <Swatch hex={pair.dark} theme="dark" />
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Accents */}
        <div className="rounded-2xl border border-black/[0.06] bg-white p-5">
          <h3 className="text-[14px] font-semibold text-[#17181A]">Accents</h3>
          <div className="mt-4 flex flex-wrap items-start gap-x-4 gap-y-3">
            <AccentSwatch label="Brand" hex={BRAND.light} brand />
            {ACCENTS.map((accent) => (
              <AccentSwatch key={accent.id} label={accent.label} hex={accent.hex} />
            ))}
          </div>
          <p className="mt-4 text-[11px] leading-relaxed text-[#9AA0A6]">
            Brand shifts to {BRAND.dark} on dark. Accents tint avatars, bubbles, badges and pills
            inline.
          </p>
        </div>

        {/* Type ramp */}
        <div className="rounded-2xl border border-black/[0.06] bg-white p-5">
          <h3 className="text-[14px] font-semibold text-[#17181A]">Type ramp</h3>
          <div className="mt-4 flex flex-col gap-3">
            {TYPE_RAMP.map((row) => (
              <div key={row.label} className="flex items-baseline justify-between gap-3">
                <span
                  className={cn("text-[#17181A]", row.weight)}
                  style={{ fontSize: row.size, lineHeight: 1.2 }}
                >
                  {row.sample}
                </span>
                <span className="shrink-0 text-[11px] text-[#9AA0A6]">{row.size}px</span>
              </div>
            ))}
          </div>
        </div>

        {/* Shape & motion */}
        <div className="rounded-2xl border border-black/[0.06] bg-white p-5">
          <h3 className="text-[14px] font-semibold text-[#17181A]">Shape &amp; motion</h3>
          <div className="mt-4 flex items-end gap-3">
            <div className="grid h-16 flex-1 place-items-center rounded-3xl bg-[#F0F0EE]">
              <span className="text-[11px] font-medium text-[#6B6F76]">rounded-3xl</span>
            </div>
            <div className="grid h-16 w-20 place-items-center rounded-full bg-[#059669]">
              <span className="text-[10px] font-semibold text-white">pill</span>
            </div>
          </div>
          <p className="mt-4 text-[11px] leading-relaxed text-[#9AA0A6]">
            springs · 220ms shared axis · {DEVICE_SLOTS} device slots
          </p>
        </div>
      </div>
    </section>
  )
}

function Swatch({ hex, theme }: { hex: string; theme: "light" | "dark" }) {
  return (
    <span className="flex flex-col items-center gap-1">
      <span
        className="block h-8 w-8 rounded-lg border border-black/10"
        style={{ backgroundColor: hex }}
        aria-hidden
      />
      <span className="text-[9px] tabular-nums text-[#9AA0A6]">
        {hex}
        <span className="sr-only"> {theme} theme</span>
      </span>
    </span>
  )
}

function AccentSwatch({ label, hex, brand }: { label: string; hex: string; brand?: boolean }) {
  return (
    <span className="flex flex-col items-center gap-1">
      <span
        className={cn("block h-7 w-7 rounded-full", brand && "ring-2 ring-[#059669]/30 ring-offset-2")}
        style={{ backgroundColor: hex }}
        aria-hidden
      />
      <span className="text-[9px] text-[#9AA0A6]">{label}</span>
    </span>
  )
}
