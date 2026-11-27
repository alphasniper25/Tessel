# TESSEL 1.1.0-alpha.1 — source-only development checkpoint

## Implemented
- Three fixed paid card slots: five rarity tiers, single-use purchases, escalating coin refresh, individual free lock/unlock, price freeze and carry-over between stages. Empty/sold slots stay empty until refresh or the next stage. Separate healing and existing boss perk rewards remain.
- Four selectable solo character templates (Balanced, Vanguard, Striker, Ram), starting health/speed/damage, family-first weighted drafting and two new cards: Ram Damage and Heal Power. Ram is contact retaliation when hurt by an enemy, not a dash or invulnerability. All characters remain eligible for all available upgrade families; rarity is independent. Character identity and lock state are checkpointed.
- Template architecture: core/content/BuildCatalog.kt holds class stats, family weights, rarity probabilities and tier size/premium; Upgrades.kt holds stable IDs, effects, prices and families. Adding a card to a family does not inflate that family's roll chance. Card IDs encoded as 16-bit offers (8-bit ID + tier); append, never reorder. This is a typed Kotlin catalog, not a runtime JSON mod system.
- Host-side lock/unlock, purchase and refresh validation, ready-state protection, per-player shop snapshot and immediate checkpoint callback for accepted remote shop changes. Wire protocol 14; original v13 peers cannot join. Checkpoint format 4 migrates earlier stock and stats; former five slots reduce to the first three. Old saves default to Balanced. Original GitHub updater disabled until the fork owns a channel.
- Updated source version to 1.1.0-alpha.1 (22); original MIT license retained. No APK, no release tag, no signing, no build outputs in archive.

## Verification
See ALPHA_CHECKS.md for reproducible JVM smoke results. The Android Gradle build, Compose UI, full original test suite, real-device multiplayer, balance in complete runs, and original save migration on an Android device are not verified. Shop and character tests were adapted to the new design. Application ID and Nearby service ID remain upstream values for now; select distinct fork identifiers before a release. The source must not be published under upstream's identity.

## Next increments
1. Lobby character selection for each player, host-validates template IDs, seats persist them across reconnect/restart; solo choice is present now, new co-op seats still start Balanced. Add custom point-budget builds after preset balancing. Improve long-run ram mechanics, including actual dash if wanted, rather than only contact retaliation.
2. Personal account XP and levels. Extend the existing ledger with idempotent run IDs, XP formula based on cleared stages/bosses and completed/failed runs, post-run display, save/resume and duplicate-award tests. Developer/practice/duels should not earn run XP. Unlock cosmetics and class options first; avoid permanent combat advantages in PvP. No XP awarded by this alpha.
3. Boss phase and attack template refactor, fair warnings, dodge windows, new patterns and repeated-fight variety. Enemies and original boss combat patterns remain unchanged in this alpha.
4. More multiplayer regressions (disconnects, lock/refresh race, slow and duplicate actions, 2/4/8 clients), profile progression and class negotiation. No internet multiplayer or full anti-cheat claims.

## Source-only development policy
- GitHub becomes source of truth once the repository is accessible. Use feature branches, review, test results and commits. Never build APKs or publish GitHub releases unless the owner explicitly requests a change in workflow. Full JVM/Gradle unit tests are allowed; no assemble/distApk tasks.
- Do not upload secrets, local.properties, signing keys or build caches. Preserve attribution. Keep the original ZIP and checkpoint releases on Drive.
