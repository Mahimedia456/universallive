UNIVERSAL LIVE — OAUTH ENABLED PARAM HOTFIX
===========================================

CAUSE
-----
OAuth patch calls:

    UlSecondaryButton(..., enabled = !oauthBusy)

but the shared UlSecondaryButton composable did not expose an `enabled`
parameter. Kotlin therefore failed with:

    No parameter with name 'enabled' found.

FIX
---
UlSecondaryButton now has:

    enabled: Boolean = true

and passes it through to OutlinedButton.

This is backward compatible with every existing UlSecondaryButton call.

MERGE TARGET
------------
E:\UniversalLive

VERIFY
------
cd E:\UniversalLive

.\gradlew.bat `
  :composeApp:compileKotlinMetadata `
  :androidApp:compileDebugKotlin

Then:

.\gradlew.bat :androidApp:assembleDebug

Expected:
BUILD SUCCESSFUL

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

NO SQL / BACKEND / ADMIN CHANGES
--------------------------------
This hotfix changes only the shared mobile Compose button primitive.
