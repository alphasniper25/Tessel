# TESSEL fork development instructions

## Baseline and intent
- Source supplied by the project owner: Tessel 1.0.1 (versionCode 21), copyright David Steenvoorden, MIT. Preserve LICENSE and upstream attribution. Approved scope: three paid shop cards with refresh/locks, weighted class builds, personal progression, boss improvements and multiplayer improvements. See development/CHANGELOG-1.1.0-alpha.10.md for what is implemented versus planned.
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
- Stable upgrade/enemy/perk numeric IDs are serialized; append rather than reorder. Check checkpoint FORMAT=7, ledger FORMAT=1, and Wire.VERSION=24 before making compatibility changes. Migrate or explicitly reject incompatible saves and peers. Cover both host and client in every new gameplay feature. Preserve solo play, team play, duels, touch and controller input.
- Use existing fixed-step simulation, pooled objects and deterministic RNG. Avoid allocations or blocking in per-frame loops; do not add unrequested cloud accounts or external services.
- Default workflow: source updates only. The owner explicitly authorized a debug test APK for alpha.3. Run `gradlew.bat testDebugUnitTest` (or `./gradlew testDebugUnitTest`) when the pinned SDK/toolchain are available, plus focused standalone JVM checks; build `assembleDebug` only when explicitly requested. Do not invoke assembleRelease, distApk or publish releases without approval. Do not claim APK readiness from JVM-only tests.
- For any requested feature: identify affected files, add or adjust focused tests, run the regression suite, record test outputs and limitations, then update changelog. Keep unrelated behavior unchanged. No release without actual build and signing review.

## Alpha.4 visual constraints
- Exactly three paid shop slots; never add a fourth. Preserve the original flat, faceted geometric 2D style. No 3D assets, new arena obstacles or glow-heavy redesign.
- Use GarageCatalog and TankArt for reusable cosmetic parts and previews. Cosmetics never change physics, stats, weapons or collision. Credit balance is derived from completed-run ledger totals, minus owned cosmetic prices; no run coin conversion. IDs and per-slot wire order append only.
- Viewport zoom is local rendering only. Preserve all-player readiness, NFC, direct Wi-Fi and compressed snapshots. Save FORMAT=7 reads older saves; peers use Wire.VERSION=24.

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

## Alpha.10 physical drones
- DroneSpec and DroneWing own all friendly drone limits; maximum eight live units per player, fixed slots, host-authoritative contact, serial respawn from reload, no drone bullet emission. Preserve bounded snapshots and checkpoint FORMAT=7. Both multiplayer peers need wire 24. Camera follows local player; default 1.60, close 1.85, full arena 1.0. No automatic difficulty changes based on player skill. Alpha.9 Arsenal other frame values remain. Source-only work unless APK explicitly requested.

## Alpha.11 progression and camera
- Skill IDs 10–18 are appended perk bits; original boss perks remain 0–9. Skill points are earned at stages 7/13/19/26/32/38/44 and recovered from cleared-stage count on older saves; maximum seven points in a normal run. Three independent ranks per branch. No account grind, no new serialized field. Core theft excludes skill bits.
- Reserve upgrade IDs 20–22 are appended; available only from stage 15. Checkpoint FORMAT=7 uses its existing upgrade-count header; wire version 25 rejects old peers. Preserve three paid slots, locks, free/paid reroll accounting, and local camera only in multiplayer.
- Assemble only debug APK as an intermediate after unit tests pass; never deliver until pinned permanent test signer, package, monotonic versionCode and APK hash are independently verified.

## Alpha.12 — audio, codex and progression visibility
- Preserve the original geometric engine and existing save layout, with test application ID unchanged. VersionCode 33; Wire.VERSION 26 deliberately rejects mixed alpha.11/12 peers because drone balance and skill timing changed.
- First run skill point moves from stage 7 to 3; other six milestones unchanged. Old saves never lose a point. Existing mask, host validation and fixed three shop slots preserved. Show the full tree from home and every shop; pause allows inspection only. No account power grind.
- Codex filters and read-only previews use all actual frame definitions. CodexDemo is an isolated deterministic World with no persistence, scoring or networking; one preview at a time, loop after 480 ticks. Keep live camera separate.
- Original synthesized effects only, per-event priorities/rate limits, master volume and menu sound preference. No licensed music.
- Drone HP/caps unchanged. Invulnerable boss states excluded from contact; small provisional balance changes to reach, damage, speed and refill.
- Run full Android unit suite then assemble debug; permanent test signing must match the pinned original certificate and version/package check. Never expose private backup/password or publish the temporary CI APK.

## Alpha.13 — constellation tree (source only)
- Replace milestone skill prompts with an optional 60-node, six-sector connected graph. Run coins pay fixed prices, minimum cleared stage 3; stage gates and adjacency checked by the host. A player may branch or cross through six bridges, or skip all skill spending.
- Home skill map is preview-only; pause inspects current run; purchase only from the shop, never automatically open. Preserve boss evolutions/perks, three paid cards, locks, prior balance configuration except explicitly enumerated graph bonuses. Keep separate free/paid reroll counters.
- Stable graph IDs 0–59 and seat masks (long); migrate format 1–7 checkpoints to FORMAT=8, preserve legacy purchased ranks without charging coins, remove unclaimed old milestone prompts, carry remaining free refresh accounting. Wire.VERSION=27, action BUY_SKILL=13, host validates seat/readiness/purchase. Alpha.12 and 13 are not multiplayer-compatible. 
- Arsenal is the new displayed name for the existing tank/card/mission collection; preserve all 33 live previews and related filters. Main menu sequence Play, Play Together, Tank Garage, Skill Tree, Arsenal, Statistics, Settings.
- Owner explicitly requests NO APK for this candidate. Only testDebugUnitTest in source workflow; no assemble, sign, publish or release. VersionCode 34 reserved for future authorized build.

## Alpha.14 — final usability, rendering and defense (source only)
- Full-screen responsive run setup, fixed title/footer, scrollable content independent of the primary action; preserve all difficulties, starting frames, five save slots and confirmation behavior. Existing Arsenal live tank previews unchanged.
- PerformancePreset drives presentation only: Battery saver 30 FPS, Controlled 60 FPS, High quality display-native. Independent FPS/effects/audio/vibration overrides persisted; no enemy-count, simulation-rate, hitbox, difficulty or network-rate changes. RenderPacer gates canvas draws, never simulation/host pump. Native rate is a platform request, not guaranteed FPS.
- Drones remain capped at 8 per player. Idle guard can move toward incoming projectile on a bounded cadence; manual aim commands all drones. Swept first-contact interception consumes one drone HP; a later drone cannot block an earlier tank hit. Host decides projectile damage for drone pilots to avoid prediction race; ordinary remote player prediction retained. Wire 28.
- Append mission upgrade IDs 23–26 and mission bits 4–7; keep existing save FORMAT=8, ledger format, unlock-on-new-run behavior and 3 shop slots. Rarity and tank compatibility checked. Eight-bit rematch mask decoded unsigned. Add ledger-derived achievements only, no extra reward transaction.
- Source-only by owner instruction; versionCode 35, no assemble, signing or distribution. Record actual regression results and device-test limits.

## Alpha.15 — economy, profile and device-layout audit (source only)
- CampaignEconomy owns common price and pickup growth (1.075/1.065), interpolated stage reward anchors, difficulty payout factors and fixed graph pricing. Preserve existing wallets, spent-upgrade history, skill ownership and locked quotes; old high-denomination wallets may have temporary surplus. No migration that silently removes coins. Existing save FORMAT=8, wire 29. No dynamic difficulty adjustments.
- Keep tank stat definitions stable until matched audit data supports individual tuning. Compare all 33 frames at four difficulties with two controlled loadouts, and 33 stage-one campaigns per variant using identical seeds/policies. Controlled encounters and natural campaigns are distinct; record actual rows, not planned totals. Agent performance does not establish human balance.
- Garage append IDs 15–25 with original 32-bit mask and nibble encoding; cosmetic only. Profile uses existing local multiplayer name; Google sign-in and Google Play listing are placeholders, no cloud or identity credentials. Settings reachable through profile or pause.
- ScreenFit validates safe bounds, compact HUD and sticks without changing arena physics or personal camera. Validate multiple sizes and font scaling; do not claim perfect rendering on all phones from tests alone.
- Source only, versionCode 36; never assemble, sign, publish or generate APK without explicit request.

## Alpha.16 — interface-only discovery and portrait Arsenal
- Keep the alpha.15 gameplay economy, save FORMAT=8, protocol 29, tank stats, mission IDs and 60 skill IDs unchanged. Discovery is a pure view of each seat's skill mask: roots or neighbors of owned nodes are revealed, owned nodes filled, all other nodes anonymous silhouettes with no hit/inspect action. No new saved fields or currency.
- Skill map fills the screen; no persistent detail panel or duplicate title. Tap a revealed node for its exact current quote and effect; details close independently. Keep zoom, pan, fit, accessible actions and purchase authority.
- Arsenal alone requests sensor portrait, returns to sensor landscape on exit; existing manifest handles orientation/configuration changes, responsive wide layout fallback when Android ignores orientation. Single-column portrait list, consistent annotated tank diagram from actual base player formulas, no false weapon stats for drones/melee, automatic isolated eight-second previews with lifecycle stop. No real-game progress.
- Neutral skill-tree menu button and remove redundant center headings in collection/garage/settings/profile/stats/lobby. Keep navigational controls. Source only, versionCode 37, no APK until owner authorizes.

## Alpha.16.1 rework
- Branch test/alpha16-rework from exact alpha.16 commit 9db77a1cd5539151ac63aa9bd26c8d2ee58875f9, not .17 or .18. Preserve other branches. Version code 40 permits update over prior test APKs; alpha.17/.18 checkpoint changes not audited. No APK without explicit authorization.
- Exactly five taps with quiet window and four-digit local PIN 5144; one developer page accessible from profile beneath Settings and from Settings itself. Never use dev mutations in real campaigns/co-op; all developer sessions isolated even with default parameters. Force offers only in dev, real shop rules still validate purchases. Result/audio/haptic test controls.
- Full-screen shop is exclusively tree-map Upgrades / Skills / Perks. Three actual offers preserve rarity/ranks/cost/locks. Dim nodes show icons only; normal players cannot inspect or buy those. Perks tab contains the original mandatory reward cards plus perk graph. Skills use original graph, same wallet. All 5 combos derive from two owned perk bits, no extra serialized fields; wire 34.
- Circuit is the provisional native layout; alternatives are interactive design previews, not native builds. Source check only, no APK.
