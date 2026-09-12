UNIVERSAL LIVE — ADMIN STALE JSX RESOLUTION HOTFIX
===================================================

CURRENT BUILD ERROR
-------------------
UsersPage.tsx imports PageTitle from ./DashboardPage.

The project currently contains both:
- DashboardPage.tsx
- DashboardPage.jsx

Vite/Rollup resolves the stale DashboardPage.jsx file, which does not export PageTitle.
That produces:

"PageTitle" is not exported by "src/pages/DashboardPage.jsx"

FIX
---
This patch does NOT rewrite the current TSX pages.

It removes stale *.jsx files only when the exact same path/basename also has a *.tsx file.

Example:
DashboardPage.jsx  -> removed
DashboardPage.tsx  -> preserved

This also protects against the same problem appearing later on other pages.

MERGE
-----
Extract into:

E:\UniversalLive

RUN
---
cd E:\UniversalLive

Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "E:\UniversalLive\tools\admin-stale-jsx-hotfix-verify.ps1"

EXPECTED
--------
PASS: stale JSX/TSX collisions cleaned.
PASS: DashboardPage resolves to TSX only
PASS: Universal Live admin production build

NOTE
----
"The system cannot find the file specified" for node_modules\.vite is harmless when
that cache directory does not exist. It was not the build failure.
