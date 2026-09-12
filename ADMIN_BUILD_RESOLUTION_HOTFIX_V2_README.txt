UNIVERSAL LIVE — ADMIN BUILD RESOLUTION HOTFIX V2
==================================================

THIS PATCH FIXES THE TWO CURRENT ERRORS

1) BACKEND
----------
error TS5103: Invalid value for '--ignoreDeprecations'

Cause:
The previous compatibility script inserted:
  "ignoreDeprecations": "6.0"

The installed TypeScript compiler does not accept that value.

Fix:
- removes ignoreDeprecations completely
- keeps explicit rootDir: "./src"
- does NOT remove baseUrl or rewrite existing Nest imports

The baseUrl deprecation remains only an editor/migration warning if your installed
TypeScript reports it. It should not be converted into a broken compiler option.

2) ADMIN
--------
"authApi" is not exported by "src/api.js"

Cause:
AdminApp.tsx imports:
  ./api

The project contains the canonical TypeScript source api.ts and a stale api.js.
Vite/Rollup resolves the stale api.js first, just like the earlier
DashboardPage.jsx collision.

Fix:
- removes api.js when api.ts exists
- removes all stale *.js siblings when matching *.ts exists
- removes all stale *.js / *.jsx siblings when matching *.tsx exists
- preserves the canonical TypeScript files
- clears Vite cache
- verifies api.ts actually contains authApi

MERGE
-----
Extract into:
E:\UniversalLive

RUN
---
cd E:\UniversalLive

Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "E:\UniversalLive\tools\admin-build-hotfix-v2-verify.ps1"

EXPECTED
--------
PASS: backend tsconfig rootDir fixed
PASS: invalid ignoreDeprecations option removed
PASS: ./api resolves to api.ts only
PASS: api.ts contains authApi and stale api.js is gone
PASS: backend build
PASS: admin build
PASS: no stale JS/TS source collisions

WHY CLEAN ALL MATCHING COLLISIONS
---------------------------------
You have now hit this problem twice:
- DashboardPage.jsx shadowed DashboardPage.tsx
- api.js shadowed api.ts

So this patch fixes the source-resolution problem project-wide instead of only
deleting one filename at a time.
