UNIVERSAL LIVE — MOBILE INTEGRATION BATCH 03.1 BUILD REPAIR

MERGE INTO:
E:\UniversalLive

FIXED CURRENT BUILD ERRORS:

1.
ConnectedFinalScreens.kt:
Unresolved reference 'CaptureController'

Cause:
The file imported:
com.universallive.app.streaming.CaptureController

Actual project CaptureController is under:
com.universallive.app.streaming.capture

But ConnectedGoLiveBackendPanel did not use the controller at all.
The unused parameter/import are removed completely.

2.
RootNavigation.kt:
Too many arguments for ConnectedNotificationsScreen(...)

Cause:
ConnectedNotificationsScreen accepts:
(state, onBack)

but RootNavigation passed a third trailing lambda.
The extra lambda is removed.

BUILD:

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\mobile-integration-batch03-1-build-repair.ps1

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

NOTE:
iosX64 Compose dependency-resolution diagnostics may still print on Windows.
The Android success criteria are:
:composeApp:compileAndroidMain
:androidApp:assembleDebug
