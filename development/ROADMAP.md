> Historical initial brainstorming. Current owner-approved roadmap: CHANGELOG-1.1.0-alpha.2.md.

# TESSEL — proposed development backlog

Status: planning, not approved for implementation. Starting point: original Tessel v1.0.1, 2026-11-27.

## 0. Foundation (first)
- Keep immutable source backup and establish a reproducible debug build.
- Agree on fork name and application ID; separate updater, Nearby service identity, signing and branding. Keep MIT attribution.
- Run regression tests and test at least two Android devices.

## 1. Small gameplay improvements (choose first)
- Better shop readability: show current and next values, level/max, upgrade costs; preserve reroll/heal behavior.
- More build variety: new upgrade types, e.g. critical hit chance, regeneration or ricochet, after designing balancing and save/network compatibility.
- New run perks with meaningful trade-offs. Keep perks run-long and avoid duplicates.
- More configurable survival: stage length/pacing or a distinct endless mode after stage 50; preserve the original campaign as a selectable mode.

## 2. Larger optional expansion
- Selectable characters or starting loadouts with different stats, subject to balance.
- Permanent progression / cosmetics with explicit reward rules. Prefer local saves unless online features are expressly requested; keep original run upgrades separate.
- More enemy archetypes and boss patterns, performance tested.
- UI polish, improved game feedback, and accessibility settings; preserve the clean geometric art direction.
- Internet multiplayer, cross-platform or cloud saves only as separately scoped projects; current LAN implementation is not an online backend.

## Change log
- Development baseline: source archived, architecture inspected, Codex instructions and verification notes added. No source gameplay changes.

## Decision log
- Retain native Kotlin/Compose and existing gameplay by default; confirm first feature before modifying gameplay. All items above are suggestions, not requirements.
