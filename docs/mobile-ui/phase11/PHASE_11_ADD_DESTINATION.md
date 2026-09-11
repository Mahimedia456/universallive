# Phase 11 - Add Destination (LOCKED)

Platforms: YouTube, Facebook, Twitch, TikTok, Custom RTMP.

Current functional flow:
1. Choose platform.
2. Open platform-specific connection screen.
3. Enter RTMP/RTMPS server and stream key.
4. Save encrypted credentials through backend.
5. Open Phase 12 Destination Details.
6. Run real backend connection test there.

Later backend phase: platform OAuth/account/channel APIs plug into this same UI boundary without redesigning the navigation hierarchy.
