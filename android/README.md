# Wault — Android (Phase 1)

The Wault v2 Android foundation: a full Jetpack Compose UI (Material 3, Wault design language) driven end-to-end by a **MockEngine** behind the real `SessionEngine` interface. Zero protocol code — Phase 1 proves the product, Phase 2 swaps in WhatsApp.

## What is here

- **Compose app, single activity, MVVM + Hilt.** Nav graph with three bottom tabs (Vault, Inbox, Settings) plus full-screen Conversation and Pairing destinations, 220 ms fade/slide transitions, edge-to-edge, predictive-back enabled.
- **Wault design language** (`ui/theme/`): warm neutral light/dark surfaces from the §4 token table, brand emerald `#059669` (bright `#34D399` in dark), per-account accent palette, tight-tracking type, 24–28 dp radii, hairline borders, no shadow spam.
- **Engine contract** (`domain/engine/`): `SessionEngine` interface + sealed `EngineEvent` stream (`MessageReceived`, `ReceiptUpdated`, `ChatChanged`, `PresenceChanged`, `PairingUpdate`, `EngineError`).
- **MockEngine** (`domain/engine/MockEngine.kt`): per-account simulator. Emits chat snapshots at start, ambient incoming messages every 45–90 s, presence toggles, and on send walks receipts Sent -> Delivered -> Read (~0.8 s / ~1.8 s) then answers with a contextual canned reply ~2.5–4 s later. Unlinked accounts run the pairing state machine Waiting -> QrReady -> Linking -> Linked; the "Simulate scan" button calls `confirmQrScanned()`.
- **InboxBus + SessionDirector** (`domain/`): one merged event flow across all live sessions; director owns the accountId -> engine map. **Hot sessions only** in Phase 1 — warm/cold tiering, device tiers and the event journal are Phase 3 (documented in KDoc).
- **In-memory data layer** (`data/`): store + repository translating engine events into UI state. Demo content is mirrored 1:1 from `web/src/components/wault/data.ts` (same accounts, chats, snippets, conversation scripts), so web and Android demos feel like the same product. Room lands in Phase 2.
- **All five screens**: Vault (slot meter, account cards, add-number), Inbox (account chips, search, unified rows), Conversation (tonal accent bubbles, receipt ticks, optimistic send, composer), Pairing (stepper with QR stage), Settings (dark theme toggle persisted to SharedPreferences, engine info, vault stats, about).
- **CI**: `.github/workflows/android-apk.yml` builds a debug APK artifact on every push to `v2` touching `android/**`.

## Open in Android Studio

1. Android Studio Ladybug (or newer) with **JDK 17**.
2. Open the `android/` directory and let Gradle sync (AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9).
3. The **Gradle wrapper jar is intentionally not committed.** Generate it once with `gradle wrapper --gradle-version 8.9` (from `android/`), or just let Android Studio sync — Studio uses its own Gradle when the wrapper jar is absent. `gradle-wrapper.properties` already pins the 8.9 distribution.
4. Run the `app` configuration on an emulator or device (minSdk 26).

## Mocked vs real (Phase 1)

| Real | Mocked |
|---|---|
| Entire UI, navigation, theming | WhatsApp protocol (MockEngine simulates messages, receipts, presence) |
| Hilt wiring, event bus, session lifecycle | QR code (static placeholder glyph + "Simulate scan") |
| Dark mode, unread accounting, optimistic send flow | Persistence (in-memory store; Room in Phase 2) |
| Debug-APK CI artifact | Notifications, media, warm/cold session tiering |

## Phase 2 hook

`WhatsmeowEngine` implements the same `SessionEngine` interface (whatsmeow compiled to an `.aar` via gomobile), registered by `SessionDirector` instead of `MockEngine`. No screen changes. The engine card in Settings already names the plan; `app/proguard-rules.pro` carries keep-rule notes for the gomobile bridge.
