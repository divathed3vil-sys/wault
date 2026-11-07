# Wault v2

Multi-account WhatsApp client with a custom, professional UI — **web app live now**, native Android next. **No WebView.**

Each account runs as a real linked device (WhatsApp multi-device protocol) behind a session server. The entire interface is ours: Material 3 foundations, Apple-grade restraint, one accent per account.

| Phase | Deliverable | Status |
|---|---|---|
| 0 | Master plan + interactive design prototype + repo + Pages CI | ✅ `phase-0` |
| **W1** | **Actual web app + Wault Bridge** — real QR pairing, live messaging, SQLite persistence | ✅ **`w1`** |
| 1 | Android Compose skeleton on MockEngine + APK CI | next |
| 2 | whatsmeow engine on Android · one real account | |
| 3 | Multi-account · Unified Inbox · notifications | |
| 4 | Bridge hardening (auth, TLS, media) · Android Office mode | |
| 5 | Web polish: PWA, media, receipts | |
| 6 | Office layer | |
| 7 | Polish & private distribution | |

## Repo map

```
wault/
├── docs/wault-v2-plan.md   ← master build plan (source of truth — read this first)
├── web/                    ← the actual web app (Next.js + socket.io client)
├── bridge/                 ← Wault Bridge (bun + Baileys + SQLite + socket.io)
├── android/                ← Phase 1+ · Kotlin + Jetpack Compose app
└── .github/workflows/      ← CI: Pages deploy (now), APK builds (Phase 1+)
```

## How it works

```
Browser ── socket.io ──► Wault Bridge ── multi-device protocol ──► WhatsApp
                            ├─ one Baileys session per account (linked device)
                            ├─ real QR pairing, live message events + receipts
                            ├─ auth state on disk · messages in SQLite
                            └─ session keys NEVER leave the server
```

The web app has two modes:
- **Live** — bridge reachable: real accounts, real QR pairing, real send/receive
- **Demo** — bridge unreachable (e.g. GitHub Pages static hosting): the full UI on the mock dataset; the design-contract *Showcase* view is also one toggle away

## Run it

```bash
# 1. Bridge
cd bridge && bun install && bun run dev     # :3031

# 2. Web app
cd web && bun install && bun run dev        # :3000
```

Open the app → **Pair new account** → WhatsApp → Linked devices → scan → chat.

## Live on GitHub Pages

https://divathed3vil-sys.github.io/wault/ — the full app in demo mode (Pages is static; WhatsApp sessions need the bridge on your own box: office mini-PC or a $5 VPS).

## Process

- **One phase = one push = one tag.** Exit criteria per phase live in the plan; a phase ships only when they're all ticked and CI is green.
- `main` holds the archived Wault v0 (Flutter) for reference; `v2` is the active branch.

## Honesty box

Wault speaks WhatsApp's multi-device protocol as an unofficial linked device. That carries account ban risk. By design, Wault ships **no bulk-send or automation features**, implements only the official multi-device protocol, and warns users during pairing. Don't point it at business-critical numbers without consent.

## Security

`bridge/data/` holds WhatsApp identity keys and the message database — it is gitignored and must **never** be committed or shared.
