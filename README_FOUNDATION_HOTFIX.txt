UNIVERSAL LIVE — MOBILE UI FOUNDATION HOTFIX

THIS IS A CUMULATIVE ZIP THROUGH MOBILE BATCH 04.

FIXES BEFORE BATCH 05:
1. Uses brand-assets/app-icon-512.png as the canonical in-app logo mark.
2. Header logo is explicitly left/start aligned.
3. UniversalLive wordmark is rendered consistently beside the real icon.
4. Header remains fixed.
5. Bottom navigation remains fixed.
6. Main content area scrolls independently.
7. Home, Studio, Go Live Setup and Active Live weighted-scroll conflicts removed.
8. Auth layout gets keyboard IME padding.
9. Existing streaming engine / RTMP / MediaProjection files remain untouched.

MERGE INTO:
E:\UniversalLive

BUILD:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\mobile-ui-foundation-hotfix-build-apk.ps1

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

AFTER THIS HOTFIX IS ACCEPTED:
Proceed to Mobile UI Batch 05 — Phase 13/14/15.
