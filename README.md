# UniversalLive — Phase 20 Mobile Checkpoint

Cumulative Kotlin Multiplatform / Compose Multiplatform mobile broadcaster checkpoint through Phases 1–20.

Core path: MediaProjection + OpenGL compositor + H.264 MediaCodec + mic/internal-audio mixer + AAC + RTMP/RTMPS, with facecam, scenes, overlays, reconnect and adaptive-bitrate foundations.

## Important: AGP 9 migration

The previous `org.jetbrains.kotlin.android` conflict has been removed. `androidApp` now relies on AGP 9 built-in Kotlin, while `composeApp` remains a KMP module using `com.android.kotlin.multiplatform.library`.

## Build/install/run over Wi-Fi ADB

Your phone must already appear as `device` in:

```powershell
adb devices
```

Then run from project root:

```powershell
.\tools\phase20-build-install-run.ps1
```

The script will generate the Gradle wrapper from `%TEMP%\gradle-9.6.1` if `gradlew.bat` is not present, then check Gradle, build, install and launch the app.

Manual flow after wrapper generation:

```powershell
.\gradlew.bat help
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:installDebug
adb shell am start -n com.universallive.app/.MainActivity
```

See `docs/PHASE_18.md`, `docs/PHASE_19.md`, `docs/PHASE_20.md` and `docs/RELEASE_QA.md`.
