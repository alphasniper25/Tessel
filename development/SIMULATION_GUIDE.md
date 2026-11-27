# Reproducible synthetic beta harness

Run with JDK 17+ and Kotlin CLI from the repository root. No Android APK is needed; production `World.step` runs at 60 ticks/second. No invincibility, developer stats, forced wins, or skipped stages in the campaign harness. The separate smoke test uses fixtures to exercise otherwise rare states.

```sh
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core -name '*.kt') development/HeadlessMonth.kt development/EvolutionHazardsSmoke.kt -include-runtime -d sim.jar
java -cp sim.jar dev.david.tessel.core.sim.EvolutionHazardsSmokeKt
java -Xmx2g -cp sim.jar dev.david.tessel.core.sim.HeadlessMonthKt 1000 1 4 sim-day1 2100
java -Xmx2g -cp sim.jar dev.david.tessel.core.sim.HeadlessMonthKt 12 30 4 sim-month-pilot 2100 83 7
# Full requested 1,000-profile 30-day model (30,000 engine sessions; computationally expensive):
java -Xmx2g -cp sim.jar dev.david.tessel.core.sim.HeadlessMonthKt 1000 30 4 sim-full-month 2100
```

Arguments: profile count, simulated days, worker threads, output directory, maximum in-game combat seconds per run, optional profile-ID stride and offset. A fresh output directory avoids mixing versions. The harness writes one CSV per profile plus `sessions.csv`. Each focal profile has a deterministic seed, skill, branch preference, passive, difficulty and solo/team mix; mission totals persist across its days. Co-op is two local seats in the same engine, not real NFC, Wi-Fi, or latency testing. Time caps are recorded separately from defeats. Bot behavior is a proxy only: the experiment cannot measure subjective player enjoyment or real player retention. Use actual playtesting for both.
