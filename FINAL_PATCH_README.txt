UNIVERSAL LIVE — FINAL MOBILE + ADMIN PATCH
2026-09-12

MERGE TARGET
============
E:\UniversalLive

This patch is based on the uploaded UniversalLive(5).zip and contains the final
mobile streaming, splash/content cleanup, backend admin-auth/CORS, SQL hardening,
and verification changes.

IMPORTANT
=========
- Merge this ZIP into the EXISTING E:\UniversalLive project.
- Do not delete your local gradle\ folder. The uploaded archive did not contain
  gradle/wrapper files, while your Windows project previously built successfully.
- Do not overwrite backend\.env or any Firebase service-account secret.
- Run FINAL_2026_09_12_COMMANDS.txt after merge.

FINAL BEHAVIOR COVERED
======================
- single branded splash
- production-facing copy: no Phase XX strings shown to users
- full-frame RTMP screen/game output without center crop/zoom
- unmirrored main screen output
- automatic portrait/landscape stream output while app UI stays portrait
- primary camera live source
- multi-destination RTMP publishing
- per-destination aggregate live/reconnect state
- localhost + production admin CORS
- ADMIN_JWT_SECRET fallback to JWT_ACCESS_SECRET
- safe admin-only SQL hardening and one-time super-admin seed template
- backend/admin/android verification scripts

ANDROID APK
===========
The final Android source is included, but this execution environment does not
contain an Android SDK and the uploaded archive omitted gradle/wrapper files.
Therefore a NEW final APK cannot be truthfully compiled here. On the user's
existing Windows project, run tools\final-mobile-admin-build-verify.ps1; it
builds and copies the APK to artifacts\UniversalLive-final-debug.apk.
