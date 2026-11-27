# TESSEL alpha.9 — Arsenal beta candidate
- Adopt the exact Arsenal weapon table tested in the matched 60,077-session cohort: Twin family +12%, Machine Gun family +18% frame damage. No Access boss reductions. Mothership, Prism and Rammer unchanged.
- Direct reliable transport: 512 queued frames and 4 MiB pending-byte budget, preserving order, latest-state coalescing and bounded fallback. Regression coverage for deterministic overflow and 300-message real-socket burst.
- Venom uses a distinct green hue (115). Preserve enemy IDs, combat parameters and hazard effects.
- Difficulty regression: Easy first boss remains over 20% softer than Normal, reflecting the already-tested alpha.8/Arsenal boss HP settings. No boss HP changes.
- versionCode 30, same test application ID, peer protocol 23, checkpoint schema 7 unchanged. Install as update only with pinned permanent signer. Physical-device testing still required.
