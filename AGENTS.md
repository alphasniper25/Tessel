# TESSEL fork development instructions

## Baseline and intent
- Source supplied by the project owner: Tessel 1.0.1 (versionCode 21), copyright David Steenvoorden, MIT. Preserve LICENSE and upstream attribution. Approved scope: three paid shop cards with refresh/locks, weighted class builds, personal progression, boss improvements and multiplayer improvements. See development/CHANGELOG-1.1.0-alpha.2.md for what is implemented versus planned.
- Build on the native Android Kotlin/Compose game; do not rewrite or replace the engine unless explicitly requested.
- Work on one approved feature or coherent change set at a time. Preserve original ZIP separately. Maintain a changelog, test report and versioned source ZIP for every release candidate.

## Architecture
- `app/src/main/kotlin/dev/david/tessel/core/content/`: definitions, IDs, balance, perks, enemies and upgrades.
- `core/sim/`: deterministic 60 Hz simulation, combat, pools, enemy logic, stages, shop, checkpoints and duels.
- `core/net/`: wire protocol, host/client sessions, direct transport and prediction.
- `core/stats/`: run history, scores, achievements. `platform/store/`: Android persistence.
- `platform/game/`: game view, renderer, HUD and lifecycle. `platform/net/`: local multiplayer lobby and transport.
- `ui/screens/`: Compose menus, tutorial, shop, stats, settings and other screens.
- `platform/update/Updater.kt`: upstream GitHub updater (do not direct fork users to upstream releases).

## Compatibility and release gates
- The upstream auto-updater is disabled in this alpha. Before a release, use a distinct application ID and Nearby service ID and configure only the owner-controlled update channel. Audit NFC routing, Android provider authorities, app labels, persisted preferences and installer identity. The Kotlin namespace may stay unchanged initially. Keep private signing credentials outside git and Drive source ZIPs. Do not publish under the original developer's identity.
- Stable upgrade/enemy/perk numeric IDs are serialized; append rather than reorder. Check checkpoint FORMAT=5, ledger FORMAT=1, and Wire.VERSION=15 before making compatibility changes. Migrate or explicitly reject incompatible saves and peers. Cover both host and client in every new gameplay feature. Preserve solo play, team play, duels, touch and controller input.
- Use existing fixed-step simulation, pooled objects and deterministic RNG. Avoid allocations or blocking in per-frame loops; do not add unrequested cloud accounts or external services.
- Current user policy: source updates only. Run `gradlew.bat testDebugUnitTest` (or `./gradlew testDebugUnitTest`) when the pinned SDK/toolchain are available, plus focused standalone JVM checks. Do NOT invoke assembleDebug, assembleRelease, distApk, APK signing or publishing. Do not claim APK readiness from JVM-only tests. Real-device verification and release gates are deferred until the owner changes the source-only policy.
- For any requested feature: identify affected files, add or adjust focused tests, run the regression suite, record test outputs and limitations, then update changelog. Keep unrelated behavior unchanged. No release without actual build and signing review.
