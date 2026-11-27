# Alpha.3 verification — 2026-11-27

Kotlin CLI 1.9.0, JDK 21. Core sources compiled successfully with the prior MissionLobbySmoke, ShopAlphaSmoke and BaselineSmoke. The mission/lobby smoke passed 14/14 and shop smoke passed 16/16, including 80,000 weighted first-slot draws. The historical baseline's final count assertion (11) was stale; updated to the 17 actual cards, then passed all 5/5 checks. The core plus new PerformanceSmoke passed 7/7 checks.

```
PASS 1: busy snapshot compressed losslessly (5939 -> 3978 bytes)
PASS 2: corrupt, truncated and oversized packets rejected
PASS 3: incompressible snapshot stays raw
PASS 4: client consumes compressed host state; kill event reference retained
PASS 5: host keeps 30 Hz combat and 10 Hz pause; unpause sends immediately
  candidates: 24562 vs 700000 full-scan checks
PASS 6: grid and full-scan collision outcomes match on 2,000 randomized shots
PASS 7: typed effects preserve character stats, compound boosts, healing and health
7 performance checks passed
```

Total 42 targeted checks passed. Candidate counts measure broad-phase workload, not FPS or wall-clock time; snapshot bytes measure one seeded busy 8-player fixture, not all Wi-Fi throughput. Menu snapshot frequency measured over a deterministic 3-second interval each.

Full Android build attempted locally via `./gradlew --version` (bootstrap prerequisite); failed before compilation: `java.net.UnknownHostException: services.gradle.org`. Android SDK absent; DNS also unavailable for dl.google.com. No local APK. Full Gradle/JUnit Android UI and physical device testing are not verified. GitHub build attempted separately if workflow permissions allow.

Reproduce the core tests:

```sh
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core -name '*.kt') development/{MissionLobbySmoke,ShopAlphaSmoke,BaselineSmoke,PerformanceSmoke}.kt -include-runtime -d /tmp/tessel-alpha3.jar
for t in MissionLobbySmokeKt ShopAlphaSmokeKt BaselineSmokeKt PerformanceSmokeKt; do java -cp /tmp/tessel-alpha3.jar "$t" || exit 1; done
```
