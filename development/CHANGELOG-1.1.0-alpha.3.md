# TESSEL 1.1.0-alpha.3 — reusable content and network efficiency

- Typed stat effects centralize all 17 card types; multiple effects may be attached to a card. Runtime effect arrays are indexed by stat, with no collection filtering in the simulation. The original additive vs compound damage/movement boost rules are retained. Mission progress uses a metric template and pre-indexed mission lookup.
- Snapshot transport uses fast, lossless DEFLATE on packets of at least 768 bytes, only when at least ~12.5% smaller; no delta baseline or new external dependency. Receiver bounds inflate size and rejects malformed compressed packets. Binary protocol version 16; every peer needs this version.
- 30 Hz combat snapshot rate retained (15 Hz for slow links, as before). Paused, waiting, shop and game-over state sends at 10 Hz; phase and ready changes and remote shop transactions trigger immediate sends. Direct Wi-Fi and Nearby fallback retained. No UDP rewrite.
- Joining client bullet collision queries reuse a spatial grid and preallocated candidate buffer. Candidate sorting retains original enemy order and collision behavior; no reduction in enemy count, bullet count or simulation tick rate.
- Retain event references in host snapshots, allowing locally predicted kill effects to be deduplicated correctly.
- Debug test identity `com.kreatonik.tessel.debug` (label Tessel Test); production application ID reserved as `com.kreatonik.tessel`. Nearby service ID and NFC AID separated from upstream to avoid routing conflicts. No upstream or fork auto-update enabled. This is a new installation with a separate data directory; old app saves are not automatically imported. New peers require the same build. Existing checkpoint and ledger formats unchanged.
- Source-only default retained; user specifically requests a test APK for this update. No production signing or publication.

## Extending the content
Use `core/content/Upgrades.kt`: append stable card ID and definition, choose a family, set price/max level, attach a primary `UpgradeStat` and amount and optional secondary effect. For a mission unlock, append `CardMission` with rarity, requirement, target and `MissionMetric`. The shop uses these definitions directly; no per-card effect branch is required for supported effects. A genuinely new gameplay mechanic still needs engine code and tests. Preserve stable IDs. Character weighting is in `BuildCatalog.kt`; enemy templates and existing boss routines remain intact.

## Build
From an Android Studio terminal with JDK 21, SDK platform 37 and network access:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on every test device. Debug key is for testing only; retain a stable key locally for future update installs. No keys belong in the repository. See checks report for actual verification status.
