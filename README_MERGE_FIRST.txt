UNIVERSAL LIVE — FINAL MOBILE POLISHED BUILD FIX

MERGE INTO:
E:\UniversalLive

FIXED:
1. Android compile failure:
   unresolved kotlinx.serialization.json.JsonObject

2. Cyan primary buttons had dark/grey text.
   All primary action text is now white.

3. Disabled cyan buttons use readable white-alpha text.

4. Red LIVE/destructive filled buttons use white text.

5. Destination screen Free-plan copy clarified:
   multiple connections may be managed;
   only simultaneous LIVE count is plan-limited.

6. Existing final Profile / Channel Connections polish preserved.

7. No Android streaming engine files are replaced.

8. iOS target is preserved.

BUILD:

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\final-mobile-polish-build-debug.ps1

APK:

E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk
