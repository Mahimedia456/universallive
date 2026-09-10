# Phase 18 — Performance, Thermal & Battery Policy

- Adds an explicit mobile broadcast performance policy instead of assuming every device can sustain 1080p60.
- Recommended safe starting profiles:
  - Balanced: 720p60 around 6 Mbps
  - Quality: 1080p30 around 8 Mbps
  - Performance: 720p30 around 4 Mbps
- Adaptive bitrate remains available during network degradation.
- Thermal policy contract supports NORMAL / WARM / HOT / CRITICAL states.
- HOT/CRITICAL state is intended to reduce bitrate/FPS in later device tuning rather than letting the process crash from thermal pressure.
- Real thresholds are finalized only on Phase 20 physical-device QA because OEM thermal behavior varies.
