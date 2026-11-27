# TESSEL V1.1.0-alpha.2 (source only)

## Player progression
- Main menu Cards & Missions displays 13 starter upgrade types and four mission-gated cards, status/progress, lifetime XP and level. Existing legitimate ledger history counts retroactively; no currency purchase or permanent stat upgrade in menus. Profile is local to the install and resets with statistics erase.
- Field Medic (common, 500 cumulative coins): +2 healing pips per paid heal; Afterburner (rare, reach stage 15): +18% movement; Titan Plating (epic, 20 cumulative bosses): +4 HP and +3 retaliation damage; Core Breaker (legendary, campaign win): +80% bullet damage. One purchase each per run; all cost run coins. Mission cards use fixed rarity. All baseline cards remain eligible across rarities, and character-family weights still apply. When only mission cards remain uncapped, the shop falls back to an available rarity rather than drawing an empty slot.
- XP derived from ledger totals: floor(seconds/10) + 2*kills + 50*bosses + 500*wins. Level L starts at 50*(L-1)*L XP. Unlocks enter new-run pools only, saved runs freeze their original masks. Local multiplayer exchanges each player's unlock mask; peer profiles are trusted, not server-verified. Practice and cheat-enabled dev runs do not count.
- UpgradeDef, CharacterDef, CardRarity, CardMission, AccountProgress centralize content and progression rules; simulation consumes their stable IDs. Existing enemy and boss behavior unchanged.

## Multiplayer
- Host and every connected player must explicitly ready before starting. Lobby roster epochs invalidate readiness on membership changes and reject old choice messages. Player character selection applies to fresh co-op; original uniform PvP rules and saved-run characters retained. Host controls difficulty/duel settings; changing settings invalidates host readiness. NFC pairing, Nearby transport and direct link implementation remain intact.
- Host-authoritative between-stage readiness retained; out-of-phase shop-ready messages rejected. Rematches need readiness from every remaining linked player; preserve connection and characters, refresh mission masks from each player's local profile. Client run recording resets for a new rematch. No unilateral restart.
- Wire protocol 15; both phones must use same version. Checkpoint format 5 persists the per-seat unlock mask; prior formats 1–4 remain readable with starter pools. Ledger format unchanged.

## Verification and release policy
- See ALPHA2_CHECKS.md for actual targeted Kotlin tests. Android UI/Gradle and real-device NFC, Nearby, direct networking, and balancing not verified here. No APK assembled or signed, no release generated, no CI build configured. Upstream MIT copyright and license preserved; original update channel remains disabled. Boss redesign, custom builds, more mission content and live UI tests are next work, not included.
