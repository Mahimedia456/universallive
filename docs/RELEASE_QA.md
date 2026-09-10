# UniversalLive Android Release QA

## Build
- Gradle wrapper works without global Gradle.
- `:androidApp:assembleDebug` succeeds.
- `:androidApp:installDebug` succeeds on Wi-Fi ADB device.

## Capture
- Screen consent requested every new MediaProjection session.
- Foreground notification visible while capturing.
- Stop from app/system releases projection, virtual display, GL, codecs and AudioRecord.

## Video
- 720p30, 720p60, 1080p30 tested first.
- 1080p60 only enabled on devices that sustain it without severe thermal throttling.
- H.264 frame and byte counters continuously increase.

## Audio
- Mic-only works.
- Internal audio works for capturable apps.
- Apps that block playback capture do not crash UniversalLive.
- Mixed AAC frame/publish counters increase.

## Network
- RTMP and RTMPS tested.
- Disconnect/reconnect tested.
- Adaptive bitrate tested under constrained Wi-Fi.

## Composition
- Facecam front/back.
- Mirror toggle.
- Scenes switch without encoder restart.
- Text/logo/image overlays render and missing assets fail safely.
