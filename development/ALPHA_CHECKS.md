# Alpha targeted checks

Actual command, run from project root with Kotlin CLI 1.9 and JDK 21:

```sh
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core/{sim,content,rng,net} -name "*.kt") -d /tmp/tessel-core.jar
kotlinc -cp /tmp/tessel-core.jar development/ShopAlphaSmoke.kt -include-runtime -d /tmp/tessel-shop-checks.jar
java -cp /tmp/tessel-shop-checks.jar:/tmp/tessel-core.jar ShopAlphaSmokeKt
```

Core compilation succeeded; compiler output empty.

```text
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
  Balanced: families=20.0%, 20.2%, 20.2%, 19.8%, 19.8%; rarity=50.4%, 27.1%, 14.8%, 6.7%, 1.0%
  Vanguard: families=10.8%, 38.3%, 25.7%, 11.4%, 13.9%; rarity=50.4%, 27.1%, 14.8%, 6.7%, 1.0%
  Striker: families=39.4%, 13.3%, 13.6%, 16.8%, 17.0%; rarity=50.4%, 27.1%, 14.8%, 6.7%, 1.0%
  Ram: families=8.0%, 16.4%, 35.2%, 28.6%, 11.8%; rarity=50.4%, 27.1%, 14.8%, 6.7%, 1.0%
PASS 16: deterministic weighted sampling; rarity independent of character (20,000 first slots each)
16 targeted checks passed
```

Family columns: gun, health, ram, mobility, economy. Rarity: common, uncommon, rare, epic, legendary. Samples are first available slots with all families eligible, not a guarantee of any specific three-card shop. Complete Android build and tests not run.
