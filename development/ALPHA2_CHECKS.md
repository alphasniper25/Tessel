# Alpha.2 targeted test report

Kotlin CLI 1.9.0, JDK 21. Command from project root:

```sh
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core -name "*.kt") development/MissionLobbySmoke.kt development/ShopAlphaSmoke.kt -include-runtime -d /tmp/tessel-alpha2.jar
java -cp /tmp/tessel-alpha2.jar MissionLobbySmokeKt
java -cp /tmp/tessel-alpha2.jar ShopAlphaSmokeKt
```

Compiler exited successfully with no messages.

```text
PASS 1: new profile starts at level 1 with all mission cards locked
PASS 2: four independent mission thresholds and XP curve
PASS 3: ledger roundtrip retains progression and unlocks
PASS 4: locked mission cards never appear in 30,000 slots
PASS 5: unlocked mission cards appear only at their declared rarity
PASS 6: partially unlocked player only draws eligible mission cards
PASS 7: host keeps separate card pools per player
PASS 8: mission card purchases apply actual effects once and cost coins
PASS 9: checkpoint keeps per-player unlock mask and starting character
PASS 10: lobby requires host and every joined player, supports 8 players
PASS 11: membership change invalidates every ready flag and rejects stale packets
PASS 12: wire roundtrip preserves character, unlocks, readiness and recipient seat; rejects corrupt messages
PASS 13: shop ready and rematch actions cannot authorize the wrong phase; rematch requires both players
PASS 14: final uncapped mission card remains available even when its rarity was not rolled
14 targeted checks passed
PASS 1: exactly three unique offers
PASS 2: lock unaffordable card without spending; cannot buy without coins
PASS 3: reroll pays once and preserves locked raw card and price without duplicates
PASS 4: purchase grants correct levels once and empties slot
PASS 5: checkpoint round trip keeps locked price and purchased empty slot
PASS 6: locked quote persists into next shop
PASS 7: new run clears prior locks
PASS 8: all three locked disables reroll
PASS 9: network snapshot preserves prices, levels, rarity, and locks for recipient
PASS 10: host rejects invalid actions, honors unlock and purchase only in shop, freezes ready players
PASS 11: old format-3 save migrates rare card, coins, levels, and balanced default
PASS 12: character stats and checkpoint identity
PASS 13: Ram contact hits enemy and still costs the player health
PASS 14: heal power increases paid healing without exceeding max HP
PASS 15: all sold slots remain empty on restore
  Balanced: families=19.9%, 20.0%, 20.5%, 19.6%, 20.0%; rarity=50.2%, 27.2%, 14.7%, 6.9%, 1.1%
  Vanguard: families=11.3%, 38.8%, 24.8%, 11.2%, 13.9%; rarity=50.2%, 27.2%, 14.7%, 6.9%, 1.1%
  Striker: families=40.4%, 12.9%, 13.0%, 17.0%, 16.6%; rarity=50.2%, 27.2%, 14.7%, 6.9%, 1.1%
  Ram: families=8.3%, 16.8%, 35.1%, 28.3%, 11.5%; rarity=50.2%, 27.2%, 14.7%, 6.9%, 1.1%
PASS 16: deterministic weighted sampling; rarity independent of character (20,000 first slots each)
16 targeted checks passed
```

Not performed: full Gradle/Android build, Android UI and instrumented suite, physical-device Nearby/NFC testing, balancing. No APK generated.
