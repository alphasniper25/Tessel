# TESSEL fork development instructions

## Baseline and intent
- Source supplied by the project owner: Tessel 1.0.1 (versionCode 21), copyright David Steenvoorden, MIT. Preserve LICENSE and upstream attribution. Approved scope: three paid shop cards with refresh/locks, weighted class builds, personal progression, boss improvements and multiplayer improvements. See development/CHANGELOG-1.1.0-alpha.9.md for what is implemented versus planned.
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
- The upstream auto-updater is disabled. Fork application ID, Nearby service ID and NFC AID are distinct; the Kotlin namespace is unchanged. Before production releases, configure stable owner-controlled signing and enable only the owner-controlled update channel. The Kotlin namespace may stay unchanged initially. Keep private signing credentials outside git and Drive source ZIPs. Do not publish under the original developer's identity.
- Stable upgrade/enemy/perk numeric IDs are serialized; append rather than reorder. Check checkpoint FORMAT=7, ledger FORMAT=1, and Wire.VERSION=23 before making compatibility changes. Migrate or explicitly reject incompatible saves and peers. Cover both host and client in every new gameplay feature. Preserve solo play, team play, duels, touch and controller input.
- Use existing fixed-step simulation, pooled objects and deterministic RNG. Avoid allocations or blocking in per-frame loops; do not add unrequested cloud accounts or external services.
- Default workflow: source updates only. The owner explicitly authorized a debug test APK for alpha.3. Run `gradlew.bat testDebugUnitTest` (or `./gradlew testDebugUnitTest`) when the pinned SDK/toolchain are available, plus focused standalone JVM checks; build `assembleDebug` only when explicitly requested. Do not invoke assembleRelease, distApk or publish releases without approval. Do not claim APK readiness from JVM-only tests.
- For any requested feature: identify affected files, add or adjust focused tests, run the regression suite, record test outputs and limitations, then update changelog. Keep unrelated behavior unchanged. No release without actual build and signing review.

## Alpha.4 visual constraints
- Exactly three paid shop slots; never add a fourth. Preserve the original flat, faceted geometric 2D style. No 3D assets, new arena obstacles or glow-heavy redesign.
- Use GarageCatalog and TankArt for reusable cosmetic parts and previews. Cosmetics never change physics, stats, weapons or collision. Credit balance is derived from completed-run ledger totals, minus owned cosmetic prices; no run coin conversion. IDs and per-slot wire order append only.
- Viewport zoom is local rendering only. Preserve all-player readiness, NFC, direct Wi-Fi and compressed snapshots. Save FORMAT=7 reads older saves; peers use Wire.VERSION=23.

## Permanent test APK signing — mandatory distribution gate
- The original alpha.3 CI debug signing key was ephemeral and is not recoverable from the APK. The initial alpha.4 CI artifact likewise uses a different ephemeral key. Do not redistribute those as update-compatible packages.
- The user-approved permanent TEST signing identity for `com.kreatonik.tessel.debug` is certificate SHA-256 `9fd3c6ade8dfb2b7d9912f0c6f028d810aed8b9fa5914eee594ee992b91fdc19`. The owner retains the encrypted keystore and recovery password in private storage. Do not commit keys/passwords or add them to public Actions artifacts, logs, or source archives. Never create a replacement key silently.
- For every explicitly requested APK, build using Gradle, then sign the test APK with this same private key using official Android apksigner. Verify signature, match the pinned certificate fingerprint, confirm the package ID is unchanged and versionCode increases relative to the distributed predecessor, record the APK SHA-256, and only then hand it to the owner. If any gate fails, report the failure instead of distributing. CI's ordinary debug APK is an intermediate artifact only, not the signed handoff. Signing occurs locally with private key access, not in the current GitHub workflow.
- Users who installed an older ephemeral-key build need a one-time migration: back up and verify private app data with ADB run-as before any uninstall; restore it after installing the stable-key APK. Never claim an in-place update from a mismatched signer. Maintain backward-readable save formats. Actual phone migration has not yet been verified.
- Use a separate production signing identity for any future public release.

## Alpha.6 balance
- Preserve serialized RAM ordinal (displayed as Rapid); no innate retaliation. Stage reward uses only per-stage deltas, credited once at shop entry. No extra reward when restoring a checkpoint. Arena 1920 × 864; Wire.VERSION=18, checkpoint remains 6. Preserve permanent test signing.

## Alpha.7 evolution and campaign constraints
- User explicitly requested source-only development for this candidate: no APK assembly, signing, or publishing. Android unit testing is permitted without APK assembly.
- Exactly three paid shop slots remain. Evolution selection is a separate free boss reward; four initial branches plus Stay Basic are valid at stage 5. Keep stable serialized evolution and upgrade IDs.
- TankEvolution and CampaignScaling are the content templates; use them for new branches and balancing. Multishot and Velocity IDs are legacy-only in new shop offers. Preserve paid legacy levels until evolution conversion.
- Keep 25-second ordinary waves, four-second cleanup, pooled entities, compact snapshots, status timers, and bounded CoreHazards pool. The protocol enemy type field has reached 32 types; widen it and bump the wire version before appending more.
- Use HeadlessMonth.kt and EvolutionHazardsSmoke.kt for reproducible simulation. Synthetic agents are not humans; report actual session counts, not planned 30,000-session capacity. Do not equate headless performance with device FPS.

## Alpha.9 beta
- Arsenal is the selected balance configuration: exactly +12% Twin and +18% Machine Gun frame damage relative to alpha.8. No Access boss reductions. Keep checkpoint FORMAT=7, peer protocol 23; test APK versionCode 30. Owner explicitly requested one permanently signed APK for this candidate.
- Do not distribute CI temporary-key APK. Verify the pinned signing fingerprint, package and version code on the actual deliverable.
