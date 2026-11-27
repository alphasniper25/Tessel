# TESSEL 1.1.0-alpha.5 — compact landscape UI

- New Run is a fixed footer, outside either scroll pane. Setup and saved runs use proportional width. The preview has compact numeric stats and uses the existing real 2D tank art. The launch footer repeats the selected difficulty/class and explains full save slots.
- Home Play is outside overflow; other menu actions fit into a compact navigation grid. The same compact tank preview is used in the co-op lobby. Saved run details sit above their action buttons rather than competing for width.
- Shop heading and Heal/Refresh/Next Stage or Ready are outside the scrollable card area. Exactly three cards, all prices and effects unchanged. Garage purchase/equip is outside the scrolling content. Run-over actions are separated from scrollable stats; pause stats can scroll.
- Retain the original colors, geometric artwork, controls and simulation. No asset redesign. Wire 17, checkpoint 6, ledger 1 and package identity unchanged. versionCode 26.
- Add short-landscape (740x320dp) Compose regression asserting New Run is displayed and above the bottom edge after class selection without scrolling. Physical-device checks still required.
- Build only as an explicitly requested test APK; use the existing permanent test signing key and verify certificate, package and version before distribution.
