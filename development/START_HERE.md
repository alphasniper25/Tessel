> Historical baseline description, not the current source state. For current status and source-only policy, see CHANGELOG-1.1.0-alpha.1.md and ALPHA_CHECKS.md. Do not follow historical APK build instructions.

# TESSEL development baseline — 2026-11-27

## Original snapshot
- Tessel 1.0.1, versionCode 21. Archive SHA-256: `6723c08b7098450f437d410e3f4b87abc8bcc26682a5ea74bedec9a9527dad94`.
- 93 Kotlin files under app/src/main; 38 test Kotlin files, 288 @Test occurrences (243 in core). Test counts are static, not passed-test counts.
- Android min SDK 26, compile/target SDK 37. Source pins Android Gradle Plugin 9.4.0, Kotlin 2.2.10, Gradle 9.7.1, Compose BOM 2026.08.00 and Nearby 19.4.0. JDK 17+ per upstream README. Toolchain availability not verified online.
- Source includes Gradle wrapper, README, MIT license, and renderer-generated screenshots in docs/.

## Game flow
60-second survival stages, eight-second cleanup, between-stage shop; bosses every five stages; two-phase Core ends stage 50. 11 repeatable upgrade types, 10 run-long perks, four difficulty settings, elite enemy modifiers from stage 6, rare three-level upgrades from stage 4. 26 bestiary entries including enemies and bosses. Local co-op up to eight, direct LAN acceleration with Nearby fallback, revive, shared coins and duel mode. Solo and co-op checkpoint saves, five solo save slots, tutorial, statistics, achievements, controller support. The source README describes device testing with only two phones and multiple features not yet tested on hardware.

## Start development on Windows
1. Extract the development ZIP to a local working directory (not a live Drive sync directory for build caches).
2. Open the tessel-main folder in Android Studio or VS Code with Codex. Open AGENTS.md and this file first.
3. Set up JDK and Android SDK platform 37; configure local.properties/ANDROID_HOME via Android Studio; do not commit local paths or keys. Use the wrapper's pinned versions.
4. From the project root run `gradlew.bat testDebugUnitTest` and `gradlew.bat assembleDebug`. Do not change dependencies simply to bypass build errors. Record failures verbatim.
5. Debug APK should appear under app/build/outputs/apk/debug/ if successful. Use a test phone; do not overwrite original release installation. Test solo, shop, resume and co-op with two phones.
6. Agree on first feature scope with the project owner. Use AGENTS.md for regression and compatibility requirements.

## Fork-specific technical risks
Upstream applicationId `dev.david.tessel`; debug suffix `.debug`. Nearby service ID `dev.david.tessel.coop`. Updater calls `https://api.github.com/repos/davidsteenvoorden/tessel/releases/latest`. These are still unchanged in this baseline. A public fork needs its own identity, update channel, signing key and branding decisions. Upstream MIT copyright and license must remain. Check checkpoint, ledger and network compatibility before extending content. Co-op accepts client-side hit reporting; it is intended for local trusted play, not a competitive internet service.

## Status
This preparation adds developer instructions, planning and a standalone optional smoke-check file only. Original app sources and Gradle configuration are unchanged. See BASELINE_CHECKS.md for verification results. No feature or public release has been approved.
