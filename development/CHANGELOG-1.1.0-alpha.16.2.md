# Alpha.16.2 — original card shop rollback

Base: original alpha.16 source 9db77a1c and developer-only checkpoint 4f01de0d. Build 42, permanent test signer required. Original three-card shop, rarity, locks, economy, skill map, 60 node IDs, perk/evolution behavior and wire protocol preserved. Only sandbox options and their integration are carried forward: five taps, PIN, trial, exact cards/ranks/skills, starting frame, live wallet and HP, result/sound/haptic tests, safe sandbox exit and isolated logbook. Format 8 remains the write format; format 9 can be read with rework-only perks and reward history discarded. Never install an ephemeral CI-signed APK over the existing game.

Validation: full Android test suite and debug APK build required before handoff; actual device installation unverified.
