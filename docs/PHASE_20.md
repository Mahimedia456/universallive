# Phase 20 — AGP 9 / KMP Build Migration + Device QA Checkpoint

## Build migration completed

- Removed `org.jetbrains.kotlin.android` from `androidApp`; AGP 9 has built-in Kotlin support.
- Removed the obsolete Kotlin Android plugin declaration from the root build and version catalog.
- Preserved separate `androidApp` application module + `composeApp` KMP library architecture.
- `composeApp` uses `com.android.kotlin.multiplatform.library` and the modern `kotlin { android { ... } }` DSL.
- Android app remains `com.universallive.app`.
- Java/Kotlin JVM target is effectively 17 through Android compile options and AGP built-in Kotlin defaults.

## Physical-device QA order

1. Generate wrapper with the supplied PowerShell script.
2. Run `gradlew help`.
3. Run `gradlew :androidApp:assembleDebug`.
4. Install to the already-connected Wi-Fi ADB device.
5. Launch `com.universallive.app/.MainActivity`.
6. Test permission flow.
7. Test screen capture.
8. Test mic and internal audio counters.
9. Test H.264 encoded frame counters.
10. Add an RTMP destination and verify CONNECTING -> LIVE.
11. Verify AAC published counters.
12. Verify facecam and scene switching.
13. Leave stream running and check reconnect/background behavior.
14. Stop stream and confirm foreground notification/service cleanup.

Compile/runtime errors found during this physical QA should be fixed against this Phase 20 checkpoint, not an older ZIP.
