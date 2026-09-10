UniversalLive Phase 30.3 Auth Cleanup

Purpose:
- Deletes stale supabase-jwt.guard.ts that re-exported deleted SizeME guard.
- Deletes any remaining SizeME-named backend TypeScript files.
- Keeps UniversalLive SupabaseAuthGuard as the actual auth guard.
- Clears dist and runs npm run build.

Run from E:\UniversalLive:
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\phase30_3-auth-clean-build.ps1

After BUILD SUCCESSFUL / Backend build succeeded:
.\tools\phase30-run-all.ps1
