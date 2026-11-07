# WAULT v2 — Master Build Plan

> Multi-account WhatsApp client for Android. Custom UI first. No WebView. Built from zero. Web edition second.
> **This document is the source of truth.** Every phase ends with a push to GitHub and an update to this file.

---

## 0. Status board

| Phase | Deliverable | Status |
|---|---|---|
| **0** | Master plan + interactive prototype + repo + Pages CI | ✅ shipped — `phase-0` |
| **W1** | **The actual web app + Wault Bridge** — real QR pairing, live send/receive, SQLite persistence | ✅ shipped — `w1` |
| **1** | Android Compose foundation on MockEngine + debug-APK CI | ✅ shipped — `phase-1` |
| **2** | whatsmeow engine · one real account end-to-end | |
| **3** | Multi-account · SessionDirector · Unified Inbox · notifications | |
| **4** | Wault Bridge hardening (auth, TLS, media) · Android Office mode | |
| **5** | Web polish: PWA, media, receipts UI | |
| **6** | Office layer | |
| **7** | Polish & private distribution | |

---

## 1. Audit of Wault v0 (what the old repo teaches us)

**Old architecture (from repo):**
- Flutter UI ("liquid glass" dark theme, WhatsApp-green primary) — a *Vault* of accounts
- Native Kotlin side: `WebViewSessionActivity0–4`, each in its **own Android process** (`:wv0`…`:wv4`), each rendering `web.whatsapp.com` in a WebView
- `WaultJsBridge` / `WaultWebViewClient` injected JS to scrape state: `qrVisible`, `loggedIn`, `unreadCount` events over an EventChannel
- `DeviceProfiler` (POTATO → BEAST tiers) + `MemoryDirector` memory policies (maxAccounts, maxWarm, freeze-to-cold)
- Account model: label + accent color + processSlot + state (COLD/ACTIVE) + unreadCount

**Why it hit a wall (and why we're glad it did):**
| Problem | Root cause |
|---|---|
| UI can never be restyled | WhatsApp Web is rendered inside the WebView — we only ever painted a launcher around it |
| Memory hell | 100–300 MB per WebView process × 5 accounts; MemoryDirector was fighting the platform |
| Fragile state | Unread counts / login state scraped via JS injection breaks whenever WhatsApp ships a UI change |
| No real notifications | WebView sessions can't produce reliable conversation notifications |
| Janky navigation | Flutter UI ↔ full-screen native activity jumps, no shared navigation graph |

**Carried into v2:** the Vault mental model, per-account identity (name + accent color), device-tier awareness (reborn as *SessionDirector*), the event-stream architecture (reborn as a message bus), and the encrypted vault concept.

**Dies with v0:** the WebView engine, JS scraping, process-slot activities, Flutter on Android.

---

## 2. The core engineering decision: kill the WebView

Wault v2 **speaks WhatsApp's multi-device protocol natively**. Each account becomes a lightweight protocol client that registers itself as a *Linked Device* (same as WhatsApp Web does today) via QR pairing.

**Recommended engine: `whatsmeow` (Go) compiled to an `.aar` via gomobile**
- ~10–25 MB RSS per account session (vs 150–300 MB for a WebView)
- Pure Go, actively maintained, battle-tested (powers mautrix-whatsapp)
- Stores device identity keys locally (SQLStore → we point it at our encrypted vault)
- First-class events: messages, receipts, presence, history sync, pairing

**Alternatives (kept on the table):**
- **Baileys (Node.js)** via nodejs-mobile — on-device, but heavier runtime + less stable than whatsmeow
- **Office bridge server** — the Web Edition's mandatory home; doubles as shared team infrastructure (Phase 4)
- **Old WebView engine** — kept behind the engine interface as a "compat mode" fallback only if ever needed

**Hard constraints we design around (honesty section):**
- WhatsApp allows **4 linked devices per account** → the Vault shows a slot meter (e.g., 2/4)
- Unofficial clients carry **account ban risk** → mitigations: official multi-device protocol only, no bulk-send / spam automation features, human-paced sending, clear in-app warnings
- On-device sockets need a **foreground service + Doze strategy** → per-account reconnect with exponential backoff, event journal for catch-up

---

## 3. Architecture (Kotlin + Jetpack Compose)

```
┌──────────────────────────────────────────────────────────┐
│ app/         Compose UI · Wault Design System · Nav       │
│              single-activity · MVVM · predictive back     │
├──────────────────────────────────────────────────────────┤
│ domain/      models · use cases · EngineEvent contract    │
├──────────────────────────────────────────────────────────┤
│ data/        Room (accounts, chats, messages, labels,     │
│              quick-replies) · DataStore (settings)        │
├──────────────────────────────────────────────────────────┤
│ engine/      SessionEngine interface                      │
│              ├─ MockEngine        (UI development — now)  │
│              ├─ WhatsmeowEngine   (gomobile aar — next)   │
│              └─ BridgeEngine      (office server — later) │
├──────────────────────────────────────────────────────────┤
│ vault/       Keystore-backed AES-GCM credential store     │
└──────────────────────────────────────────────────────────┘
```

**Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Coroutines/Flow, Room, DataStore, CameraX (QR), WorkManager + foreground service.

### The six systems we're inventing (the heart of v2)

1. **SessionEngine abstraction** — the UI never knows which protocol engine is live. One sealed `EngineEvent` contract (`MessageReceived`, `ReceiptUpdated`, `ChatChanged`, `PresenceChanged`, `PairingUpdate`, `EngineError`) drives everything. Swapping Mock → whatsmeow → bridge changes zero UI code.

2. **SessionDirector** (MemoryDirector reborn, for the protocol era) — tier-aware lifecycle management: sessions are **hot** (live socket), **warm** (paused, instantly resumable) or **cold** (keys sealed in vault, reconnect on demand). Event journal per account so cold → hot replays what was missed. Device-tier policies like v0, but now the math is on our side (10× lighter per session).

3. **Unified Inbox Bus** — all account event streams fan-in to one ordered, per-account-partitioned Flow → single `StateFlow` tree → UI. Offline-first: optimistic sends, retry queue, delivery receipts reconciled from the engine.

4. **Identity & Account Theming** — every account owns an accent from a curated palette (deterministic, user-changeable). The whole Material 3 color scheme re-tints subtly per active account context (headers, chips, outgoing bubbles) while Wault's neutral brand stays constant. This is the design signature.

5. **Vault & Trust** — per-account credential store sealed by Android Keystore (hardware-backed AES-GCM). QR pairing state machine: intro → name & color → QR scan → linking → success / device-limit error. Encrypted export/import file for migrating an office device without re-pairing everything.

6. **Notification Router** — per-account Android channels, conversation-style notifications, one smart summary per account, quiet hours, keyword rules.

---

## 4. UI — the "Wault Design Language"

Material 3 foundation, Apple-grade restraint. The rule: **WhatsApp Web's look never enters the building** — we are a vault dashboard first, chat client second.

**Principles**
- Neutral warm surfaces; exactly one accent in view at a time (brand emerald, or the active account's color)
- Type-first hierarchy: large titles, two weights, no decorative noise
- Shape: 24–28px card radii, full-pill chips, soft tonal fills (M3 containers, no harsh borders)
- Motion: quiet and springy — shared-axis screen pushes, sheets from the bottom, container-transform for pairing success
- Icons: Material Symbols Rounded (prototype uses Lucide as web stand-in)

**Tokens (light / dark)**

| Token | Light | Dark |
|---|---|---|
| background | `#F7F7F5` | `#0C0D0F` |
| surface | `#FFFFFF` | `#16181B` |
| surface-2 (tonal) | `#F0F0EE` | `#1E2126` |
| text primary | `#17181A` | `#F2F3F4` |
| text secondary | `#6B6F76` | `#9AA0A6` |
| brand (Wault) | `#059669` | `#34D399` |

**Account accent palette:** Emerald `#10B981` · Amber `#F59E0B` · Rose `#F43F5E` · Violet `#8B5CF6` · Teal `#14B8A6`

**Navigation model (phone, single-activity)**
- Bottom navigation bar, 3 destinations: **Vault · Inbox · Settings**
- Global **account switcher**: avatar cluster in the top bar → M3 bottom sheet (accounts + status + "Pair new account")
- **Inbox** is the unified list; account filter chips on top; conversations push full-screen with predictive back
- **Pairing** is a full-screen flow (stepper), bottom-to-top transition
- Tablet / fold: bottom bar becomes a nav rail, Inbox becomes list–detail two-pane

**Screens**
1. **Vault** (home) — session-health card with linked-device slot meter, account cards (status pill, unread badge, today's in/out stats, last active), "Pair new account" card
2. **Inbox** — unified chats across accounts, account-colored rings/badges, labels, pinned rows, search, new-message sheet
3. **Conversation** — account-tinted header with account chip, custom bubbles (tonal outgoing in account accent, delivery ticks), quick-reply chips, composer
4. **Pairing** — 3-step flow with live QR state and device-limit handling
5. **Settings** — appearance (theme + accent), notification rules, sessions & security (auto-lock, encrypted vault, export), about

The clickable prototype in `prototype/` is the **pixel-level spec** for these five screens — and its Web Edition shell is the spec for Phase 5's desktop client.

---

## 5. Build plan — phases, exit criteria, push checkpoints

**The contract: one phase = one goal = one push = one tag (`phase-0` … `phase-7`).** A phase is *done* only when every exit criterion is checked **and** the push checklist (§6.3) is green.

### Phase 0 — Foundations & design contract ✅ (`phase-0`)

| | |
|---|---|
| **Deliverables** | This master plan · interactive prototype (5 Android screens in a phone frame + Web Edition three-pane shell, light/dark, persisted store) · GitHub monorepo · Pages CI |
| **Exit criteria** | ☑ All 5 screens clickable with correct state transitions ☑ Pairing flow mutates accounts + slot meter ☑ Theme/view/filter survive reload (localStorage) ☑ CI builds the prototype and deploys Pages green |

### Phase W1 — the actual web app + Wault Bridge ✅ (`w1`)

*User decision:* no mockup on the web — the **real app**. That pulled the Bridge forward (the browser cannot hold WhatsApp sessions itself) and replaced the Flutter-web plan with the Next.js app that already speaks the Wault design language.

| | |
|---|---|
| **Deliverables** | `web/` — the actual Wault web client: full-viewport desktop three-pane + mobile single-column shell, live/demo dual mode (auto-fallback when the bridge is unreachable) · `bridge/` — Wault Bridge v1: multi-account Baileys sessions, **real QR pairing**, live message events + receipts, per-account auth state on disk, SQLite message store, read receipts, socket.io API (`accounts:list · account:start · account:logout · account:remove · chats:list · chats:markRead · messages:list · message:send` + pushes) · optimistic sends with tmp→real swap · unread accounting server-side · Pages CI moved to `web/` |
| **Exit criteria** | ☑ Bridge generates a real scannable QR (verified end-to-end in browser) ☑ Web app shows real vault state (0 of 4 slots, live accounts) ☑ Demo mode engages cleanly when bridge is offline ☑ Showcase (design contract) still intact behind a toggle ☑ Messages persist in SQLite; chats/read-state survive restarts ☑ No session keys ever leave the server |
| **Text-only note** | Media download/upload is W2; groups show subjects, DMs show contact names |

### Phase 1 — Android foundation on MockEngine ✅ (`phase-1`)

Shipped as a full Kotlin/Compose app under `android/` (package `com.wault.app`): Gradle 8.9 + AGP 8.7.3 + Kotlin 2.0.21 version catalog, Hilt DI, M3 theme from §4 tokens (light + dark), nav graph (3 tabs + full-screen conversation/pairing, 220 ms transitions), all five screens with ViewModels, shared components (SlotMeter, AccountCard, ChatRow, MessageBubble, StatusPill, EmptyState), `SessionEngine` + sealed `EngineEvent` + **MockEngine** (mirrors the web demo dataset), InboxBus, hot-only SessionDirector, in-memory store behind `WaultRepository`, and `android-apk.yml` (assembleDebug artifact per push). Demo dataset mirrors `web/src/components/wault/data.ts` 1:1.

Honest state: compile-verified only in CI (no Android tooling in the build sandbox) — first on-device run happens in Android Studio (`gradle wrapper --gradle-version 8.9` once if Studio asks). Web fixes verified in-browser the same push: bridge-connected mode no longer leaks demo chats, empty states tell the truth, dark/mobile shells re-verified.

| | |
|---|---|
| **Goal** | The prototype becomes a real, installable Android app — pixel-faithful, zero protocol code |
| **Deliverables** | `android/` Gradle project · Hilt app · Compose M3 theme from §4 tokens · nav graph (3 tabs + overlays + predictive back) · all five screens · **MockEngine** emitting `EngineEvent`s · CI producing a debug APK artifact on every push |
| **Exit criteria** | ☑ `assembleDebug` green in CI, APK downloadable from Actions ☐ App runs on device/emulator, all screens match the prototype (first Studio build) ☑ Dark mode + account theming live ☑ Optimistic send behaves like the prototype ☑ No protocol code anywhere (MockEngine only) |
| **Push** | tag `phase-1` + `android-apk.yml` workflow |

File map (what I'll write in Phase 1):

```
android/
├── settings.gradle.kts · build.gradle.kts · gradle/libs.versions.toml
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/app/wault/
│       ├── WaultApp.kt · MainActivity.kt
│       ├── ui/theme/         Color.kt · Type.kt · Theme.kt        ← §4 tokens
│       ├── ui/nav/           WaultNavHost.kt · Destinations.kt
│       ├── ui/screens/       vault/ · inbox/ · conversation/ · pairing/ · settings/  (Screen + ViewModel each)
│       ├── ui/components/    SlotMeter · AccountCard · ChatRow · MessageBubble · StatusPill · WaultSheets
│       ├── domain/engine/    SessionEngine.kt · EngineEvent.kt · MockEngine.kt
│       ├── domain/           InboxBus.kt · SessionDirector.kt (hot-only stub)
│       ├── data/             WaultRepository.kt · InMemoryStore.kt   (Room lands in Phase 2)
│       └── di/               EngineModule.kt · RepoModule.kt
```

### Phase 2 — Real engine, one account

| | |
|---|---|
| **Goal** | One real WhatsApp number, paired by QR, chatting end-to-end from Wault's own UI |
| **Deliverables** | `whatsmeow.aar` build (gomobile, CI job on tags) · `WhatsmeowEngine` implementing `SessionEngine` · QR pairing via CameraX + device-limit error mapping · Room v1 (accounts, chats, messages, event_journal) · encrypted SQLStore in the vault · foreground service + reconnect backoff |
| **Exit criteria** | ☐ QR scan links Wault as a device (visible in WhatsApp → Linked devices) ☐ Text + image send/receive ☐ Force-stop app → journal replays missed events on relaunch |
| **Push** | tag `phase-2` |

### Phase 3 — Multi-account

| | |
|---|---|
| **Goal** | The actual product: several numbers, one calm app |
| **Deliverables** | SessionDirector (hot/warm/cold + device tiers) · Unified Inbox Bus · Notification Router (per-account channels, summaries, quiet hours) · account theming driven by real active context · slot meter reflecting the real 4-device limit |
| **Exit criteria** | ☐ 3 accounts paired and stable ≥ 24 h on a mid-range phone ☐ Per-account notifications correct ☐ Unified inbox filters across accounts ☐ Steady-state RAM within budget (< ~400 MB) |
| **Push** | tag `phase-3` |

### Phase 4 — Wault Bridge (self-hosted server)

| | |
|---|---|
| **Goal** | One paired number, reachable from many clients — the Web Edition's mandatory home |
| **Deliverables** | `bridge/` single-binary server (Go + whatsmeow core): REST + WebSocket event stream with resume cursors · encrypted session vault (keys never leave the server) · media proxy (solves browser CORS) · token auth · Android gains `BridgeEngine` + "Office mode" toggle |
| **Exit criteria** | ☐ Number paired to bridge (1 of 4 slots) ☐ Two clients receive the same live event stream ☐ Disconnect → reconnect replays from cursor ☐ Media flows through the proxy |
| **Push** | tag `phase-4` + `bridge.yml` workflow |

### Phase 5 — Web client

| | |
|---|---|
| **Goal** | Wault in the browser, same design language, same account |
| **Deliverables** | `web/` Flutter web three-pane shell (per the prototype's Web Edition) on BridgeEngine · IndexedDB cache (Drift + sqlite3 WASM) mirroring Room · resume cursors · optimistic send queue · PWA (manifest + service worker) · hosting: served by the bridge (same-origin cookies, primary) **and** GitHub Pages with Bearer tokens (zero-cost fallback) |
| **Exit criteria** | ☐ Same number live on phone + browser simultaneously ☐ Reload restores state from IndexedDB + cursor replay ☐ Installable as a PWA ☐ Pages deployment green |
| **Push** | tag `phase-5` + Pages workflow for `web/` |

### Phase 6 — Office layer

| | |
|---|---|
| **Deliverables** | Labels · quick replies · pinned chats · global search · chat assignment (via bridge) · encrypted export/import between devices · notification rules tuned for office shifts |
| **Exit criteria** | ☐ One-week pilot checklist passes with real office usage |
| **Push** | tag `phase-6` |

### Phase 7 — Polish & private distribution

| | |
|---|---|
| **Deliverables** | R8/minify + baseline profiles · crash reporting · onboarding polish · signed release APK + GitHub Release · onboarding doc for coworkers |
| **Exit criteria** | ☐ Signed `v2.0.0` release ☐ Teammates onboarded without my help |
| **Push** | tag `v2.0.0` |

---

## 6. GitHub delivery — monorepo layout & push protocol

### 6.1 Repo layout (branch `v2`)

```
wault/                          ← github.com/divathed3vil-sys/wault, branch v2
├── docs/wault-v2-plan.md       ← this file (source of truth, updated every phase)
├── web/                        ← the actual web app (Next.js) — Pages + live with bridge
├── bridge/                     ← Wault Bridge (bun + Baileys + SQLite + socket.io)
├── android/                    ← Phase 1+ · Kotlin + Compose app
└── .github/workflows/          ← pages.yml (now) · android-apk.yml (P1) · bridge.yml (P4)
```

The Phase 0 design showcase lives inside `web/` behind the “Showcase” toggle (demo dataset, phone + framed shells). `main` keeps the v0 Flutter archive untouched; `v2` is the active branch.

### 6.2 CI inventory

| Workflow | From | Does |
|---|---|---|
| `deploy-pages.yml` | Phase 0 (now builds `web/`) | Static export of the web app (basePath `/wault`) → GitHub Pages. On Pages the app runs in demo mode; self-hosted with the bridge it goes live automatically |
| `android-apk.yml` | Phase 1 | `assembleDebug` → APK artifact on every push; signed build on tags |
| `bridge.yml` | Phase 4 | Cross-compiles the bridge binary; attaches to releases |

### 6.3 Push checklist (every phase, no exceptions)

1. All exit criteria for the phase ticked
2. Prototype CI green (Pages deploys)
3. Android CI green, APK artifact produced (Phase 1+)
4. Browser re-verification of any changed flows
5. `docs/wault-v2-plan.md` status board updated in the same push
6. Tag `phase-N` with release notes = the ticked criteria

### 6.4 Secrets & token policy

- Access tokens are **fine-grained PATs scoped to this repo only**, with `Contents: read/write` + `Workflows: read/write` (workflow files need the Workflow permission to be pushed)
- Rotate the token at each phase milestone; never commit tokens; CI only ever uses the built-in `GITHUB_TOKEN`
- A token pasted into any chat is exposed to that chat's history — paste a fresh one per session, rotate after

---

## 7. Risks (stated plainly)

- **Ban risk** — unofficial protocol clients violate WhatsApp ToS. Mitigate: official-protocol-only, no bulk/automation features, warn users, never touch office-critical numbers without consent.
- **4 linked-device limit** — Vault UX makes slots visible; "replace device" flow planned.
- **Battery / Doze** — foreground service with graceful socket handoff; catch-up journal so reconnects are boring.
- **Protocol drift** — whatsmeow tracks WhatsApp protocol changes upstream; engine isolation means UI keeps working while engine patches land.
- **gRPC/gomobile friction** — whatsmeow→AAR is the least-charted part of Phase 2; the CI-built AAR + engine interface keeps it contained.

---

## 8. Web Edition — one bridge, thin clients

> **Update (W1):** the web track is real now — the Next.js app + Baileys bridge shipped as Phase W1. Flutter web is dropped (the Next.js app already speaks the design language on every screen size). The architecture below is unchanged and is what W2+ hardens: auth, media proxy, resume cursors, PWA.

Yes, the web version is worth building — but one hard truth shapes everything:

**The browser cannot hold WhatsApp sessions.** Five reasons, all non-negotiable:
1. WhatsApp's media CDN sends no CORS headers — a browser can neither fetch nor upload media directly. A server-side proxy is mandatory.
2. The multi-device WebSocket handshake from an arbitrary browser origin is fragile; browsers force an `Origin` header that can't be spoofed, and WhatsApp can (and does) reject unknown ones.
3. Session identity keys in a browser are a liability — one XSS and your WhatsApp identity is stolen. Browser storage is not a keystore. Keys belong in a hardware-backed vault (phone) or a server vault.
4. The Go→WASM escape hatch (running whatsmeow in the browser) means 20–40 MB payloads, SQLite/fd shims and threading pain — versus a 30-line WebSocket client to a bridge.
5. GitHub Pages is static hosting — it can serve the UI, but no protocol engine can run there anyway.

**So the web edition forces the Bridge — and that's a gift, not a cost.** The office use case wanted shared infrastructure anyway:

```
Wault Bridge (Go + whatsmeow, single binary; or Node + Baileys)
  ├─ REST (accounts, chats, send, media) + WebSocket (event stream, resume cursors)
  ├─ Encrypted session vault (per-account AES-GCM, keys never leave the server)
  ├─ Media proxy + cache (solves browser CORS)
  ├─ Auth (per-user tokens, TOTP later) + FCM dispatcher
  └─ Serves the static web client (same-origin ⇒ httpOnly cookies work)
Clients — all behind the same SessionEngine interface:
  ├─ Android  → WhatsmeowEngine (Personal mode, on-device)
  │             BridgeEngine    (Office mode, connects to the bridge)
  └─ Web      → BridgeEngine only (thin client)
```

Each number pairs to the bridge **once** (consuming 1 of 4 linked-device slots) and every client — phone, browser, tablet — connects through it. Device slots stop being a per-person problem, and team features (assignment, shared inbox) become possible. The SessionEngine abstraction from §3 absorbs this with zero UI changes — Android just gains an "Office mode" toggle.

**Web client persistence stack (the "cookies & state" answer):**
- **Auth → httpOnly + Secure + SameSite=Lax cookie.** Cookies carry identity only, never data. Same-origin (bridge serves the web app) is the primary deployment; a GitHub Pages deployment is cross-origin, so it falls back to short-lived Bearer tokens + refresh in IndexedDB (third-party cookie deprecation makes cookie-based cross-origin auth a dead end).
- **Data cache → IndexedDB via Drift** (sqlite3 WASM + OPFS), mirroring the Room schema on Android: chat list, recent messages per workspace, labels, quick replies.
- **Sync → resume cursors.** Store a `lastEventId` per workspace; on reconnect the bridge replays the gap. Same journal concept as SessionDirector — one idea, two sides.
- **Optimistic sends → queue in IndexedDB** with client-generated IDs; the send endpoint dedupes idempotently; receipts reconcile.
- **PWA → manifest + service worker**, app shell precached, installable on desktop. Flutter web (CanvasKit, self-hosted fonts) renders the same Wault design language with a `NavigationRail` + three-pane adaptive shell.

**Hosting reality:** web UI on GitHub Pages — yes, free, via GitHub Actions. The bridge needs an always-on box: an office mini-PC or a $5 VPS, with Caddy for automatic TLS. Best UX is the bridge serving the web app itself (same-origin cookies, zero CORS); GitHub Pages is the zero-cost fallback with Bearer auth.

**What this changes for Android: nothing in the UI, one addition in the engine layer.** Phases 1–3 stay exactly as planned; the bridge becomes Phase 4 core, web client Phase 5.

---

## 9. What exists today (Phase 0 output)

- `docs/wault-v2-plan.md` — this document
- `prototype/` — interactive prototype of all 5 screens inside an Android phone frame (mock data, light/dark, account theming, pairing flow, animated transitions) **plus a Web Edition view**: the same store and design language rendered as a desktop three-pane shell (icon rail + workspace rail + list/detail), with theme/view/filter persisted to localStorage across reloads. It is the design contract for both the Compose and Flutter-web implementations.
- `.github/workflows/deploy-pages.yml` — builds the prototype (static export, basePath `/wault`) and deploys to GitHub Pages on every push to `v2`
- README with the phase status board

---

## 10. Immediate next actions

1. ~~Push `phase-0`~~ ✅ · ~~Build the actual web app + bridge~~ ✅ (`w1`)
2. **Try it live:** open the preview → “Pair new account” → scan the QR with a WhatsApp number → chat from the browser
3. **Phase 1 kickoff** — write the `android/` skeleton per the file map in §5 (Compose theme from §4 tokens, nav graph, five screens, MockEngine, APK CI)
4. **W2 backlog** (parallel with Phase 1): media download/upload through the bridge, presence/typing, group metadata polish, PWA manifest
5. **Rotate the earlier token** — GitHub auto-flags tokens pasted into chats; retire the old one when convenient
