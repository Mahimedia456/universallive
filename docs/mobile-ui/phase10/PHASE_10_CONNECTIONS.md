# Phase 10 - Connections (LOCKED)

Purpose: creator-facing destination dashboard for YouTube, Facebook, Twitch, TikTok and Custom RTMP.

Locked behaviors:
- Refresh saved destinations from backend.
- Fixed platform catalog always visible.
- Connected / needs test / disabled / not connected states.
- Default badge.
- Membership simultaneous-output status.
- Connected row opens Phase 12 details.
- Unconnected row opens Phase 11 connection setup.
- Add Destination opens Phase 11 platform chooser.

Backend note: direct platform OAuth/account linking is intentionally deferred to the later backend API phase. Current RTMP/RTMPS credential contract remains functional.
