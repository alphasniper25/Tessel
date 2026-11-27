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

## Alpha.8 matched 30-day deployment experiment
`HeadlessCohort.kt` runs ten motivations × five preferred branches (50 archetypes), 20 profiles per archetype for a full 1,000-player deployment. Labels are 500 female / 500 male, independent of skill, preferences and behavior. Skill improvement, session attendance, time-budgeted exits, solo/team decisions, adaptive difficulty and purchases are deterministic synthetic assumptions, not human observations. The same profile IDs and seed schedule are used across independently initialized alpha.7 and alpha.8 deployments. Prior session results can change later difficulty; comparisons are cohort-level, not a pure fixed-difficulty randomized trial. Partner seats are simulated separately within a focal player's run; they are not counted as additional profiles. Account mission totals persist within each version for 30 modeled days. No model simulates real satisfaction, retention or live-device/network performance.

```sh
kotlinc $(find app/src/main/kotlin/dev/david/tessel/core -name '*.kt') development/HeadlessCohort.kt -include-runtime -d cohort.jar
java -Xmx1g -cp cohort.jar dev.david.tessel.core.sim.HeadlessCohortKt 50 30 4 cohort-alpha8-0 2100 1 0 4
```
Last argument limits sessions/day (1–4). Full cohort, with this deterministic attendance schedule, plans 60,077 sessions per version, 120,154 for two independent deployments. The cloud workflow on `sim/alpha8` uses 20 shards of 50 profiles per version and stores real CSVs. Only count actual completed output rows, not scheduled rows. `summarize_cohort.py` merges shard summaries and reports incomplete profiles, duplicates and per-archetype metrics. Further deployments should use actual balance revisions rather than inflate the session count. Physical beta tests remain required.
