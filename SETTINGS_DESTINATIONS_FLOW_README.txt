Universal Live — Settings Destinations Flow Patch
Merge into E:\UniversalLive

Connects Settings -> Streaming Destinations to the existing real backend connection state.
Existing destinations can open Destination Details for edit/test/enable/default/delete/use-for-live.
Add New opens the existing Add Destination flow.
Back navigation returns to Settings when Connections was opened from Settings.
No SQL migration is required for this flow.
OAuth is intentionally the next workstream; existing account creation/onboarding remains unchanged.

Verify:
cd E:\UniversalLive
.\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
.\gradlew.bat :androidApp:assembleDebug

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk
