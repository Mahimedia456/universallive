UNIVERSAL LIVE — BACKEND DIST/MAIN HOTFIX
==========================================

CURRENT STATE
-------------
Admin build is now passing.

Backend TypeScript watch compilation reports:
  Found 0 errors.

But Nest/Node then fails:
  Cannot find module E:\UniversalLive\backend\dist\main

This means compilation itself is clean, but the runtime entry artifact is missing
or being emitted into the wrong folder.

MOST LIKELY CAUSES
------------------
1. Stale TypeScript incremental *.tsbuildinfo state after dist was deleted.
2. main.js emitted as dist\src\main.js while Nest expects dist\main.js.
3. tsconfig.build.json output/rootDir does not match Nest runtime expectations.

WHAT THIS PATCH DOES
--------------------
- stops backend-specific Node processes
- removes dist
- removes all backend *.tsbuildinfo files
- performs a clean npm run build
- locates emitted main.js
- if output is dist\src\main.js, normalizes tsconfig.build.json:
    rootDir: ./src
    outDir: ./dist
- rebuilds
- requires dist\main.js
- launches dist\main.js for a short runtime smoke test
- stops the smoke-test process
- does not change database schemas
- does not touch the mobile app

RUN
---
Extract into E:\UniversalLive

Then:

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "E:\UniversalLive\tools\backend-dist-main-hotfix.ps1"

EXPECTED
--------
PASS: clean backend build
PASS: dist\main.js exists
PASS: backend runtime entry starts

Then:
cd E:\UniversalLive\backend
npm run dev
