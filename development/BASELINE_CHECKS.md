# Baseline checks — 2026-11-27

- Original ZIP CRC integrity: passed (unzip -t).
- Original source version: 1.0.1 (21); source files and build configuration unchanged in development ZIP.
- Available sandbox: Java 21, Kotlin CLI 1.9.0. Android SDK and full pinned Gradle toolchain not installed/configured.
- Attempted to compile all pure Kotlin core code with the available Kotlin 1.9 compiler; failed at core/net/Pipe.kt:60 `queue.addLast(bytes)` (unresolved reference). This does not establish a defect in the pinned Kotlin 2.2.10 build. No source was modified to work around it.
- Targeted content/simulation/RNG core compiled with Kotlin 1.9 and executed the standalone BaselineSmoke.kt. 5/5 checks passed:
  - Movement at 60 simulation ticks per second.
  - Stage transitions to shop.
  - Checkpoint round trip restores coins and shop phase.
  - Next stage after checkpoint.
  - Baseline upgrade and perk counts.
- Reproduce targeted smoke test from root on a Unix shell with kotlinc installed:
  `kotlinc $(find app/src/main/kotlin/dev/david/tessel/core/{sim,content,rng} -name '*.kt') development/BaselineSmoke.kt -include-runtime -d /tmp/tessel-smoke.jar && java -jar /tmp/tessel-smoke.jar`
- Full Android Gradle unit tests, APK build, UI rendering and phone multiplayer tests remain UNVERIFIED. The 288 @Test occurrences are not a pass count. Run pinned Gradle toolchain checks on an Android development machine before modifying or releasing.
