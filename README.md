# Wault v2

Multi-account WhatsApp client with a custom, professional UI — native Android first, Web Edition second. **No WebView.**

Each account runs as a real linked device (WhatsApp multi-device protocol via [whatsmeow](https://github.com/tulir/whatsmeow)) behind a UI-agnostic `SessionEngine` interface — so the entire interface is ours: Material 3 foundations, Apple-grade restraint, one accent per account.

| Phase | Deliverable | Status |
|---|---|---|
| 0 | Master plan + interactive design prototype + repo + Pages CI | ✅ **shipped — `phase-0`** |
| 1 | Android Compose skeleton on MockEngine + APK CI | next |
| 2 | whatsmeow engine · one real account end-to-end | |
| 3 | Multi-account · SessionDirector · Unified Inbox · notifications | |
| 4 | Wault Bridge (self-hosted server) | |
| 5 | Web client (Flutter web on BridgeEngine) | |
| 6 | Office layer | |
| 7 | Polish & private distribution | |

## Repo map

```
wault/
├── docs/wault-v2-plan.md   ← master build plan (source of truth — read this first)
├── prototype/              ← interactive design contract: 5 Android screens in a phone
│                             frame + Web Edition three-pane shell; light/dark; persisted store
├── android/                ← Phase 1+ · Kotlin + Jetpack Compose app
├── bridge/                 ← Phase 4+ · Wault Bridge server (Go + whatsmeow)
├── web/                    ← Phase 5+ · Flutter web client
└── .github/workflows/      ← CI: Pages deploy (now), APK builds (Phase 1+)
```

## Live prototype

Once Pages is enabled (Settings → Pages → Source: **GitHub Actions**), the prototype deploys automatically on every push to `v2`:

**https://divathed3vil-sys.github.io/wault/**

## Development

```bash
cd prototype
bun install
bun run dev      # http://localhost:3000
bun run build    # static export → out/ (set NEXT_OUTPUT_EXPORT=1 + NEXT_PUBLIC_BASE_PATH=/wault for Pages parity)
```

## Process

- **One phase = one push = one tag.** Exit criteria for every phase are defined in the plan; a phase ships only when they're all ticked and CI is green.
- `main` holds the archived Wault v0 (Flutter) for reference; `v2` is the active branch and becomes default after Phase 1.

## Honesty box

Wault speaks WhatsApp's multi-device protocol as an unofficial linked device. That carries account ban risk. By design, Wault ships **no bulk-send or automation features**, implements only the official multi-device protocol, and warns users during pairing. Don't point it at business-critical numbers without consent.
