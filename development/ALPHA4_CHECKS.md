# Alpha.4 validation

Kotlin CLI/JVM regression: see GarageSmoke.kt and previous smoke fixtures. Garage checks cover credits, affordability, ownership, serialized selections, camera clamping, lobby/checkpoint (including format-5 migration)/snapshot appearance roundtrips and exactly three shop slots. Prior shop, mission/lobby, baseline and performance tests are retained. The final Gradle suite and physical-device UI testing are separate gates; no phone FPS or NFC validation is claimed.

Reproduce:
```
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core -name '*.kt') app/src/main/kotlin/dev/david/tessel/platform/game/Viewport.kt development/{GarageSmoke,MissionLobbySmoke,ShopAlphaSmoke,BaselineSmoke,PerformanceSmoke}.kt -include-runtime -d /tmp/tessel-alpha4.jar
for t in GarageSmokeKt MissionLobbySmokeKt ShopAlphaSmokeKt BaselineSmokeKt PerformanceSmokeKt; do java -cp /tmp/tessel-alpha4.jar "$t" || exit 1; done
```
