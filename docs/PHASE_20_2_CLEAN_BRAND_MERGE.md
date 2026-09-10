# UniversalLive Phase 20.2 Clean Brand Merge

This checkpoint contains the Phase 20.2 live monitor/preview/facecam fixes plus correctly placed Android branding assets.

## Important cleanup when merging over an existing project
Run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\cleanup-brand-res.ps1
```

The script removes invalid resource folders/files that may have been copied into `androidApp/src/main/res` from the standalone brand pack:
- `res/source/`
- `res/android/`
- root `res/app-icon-*.png`
- root `res/logo-dark.png`
- root `res/logo-light.png`
- root `res/splash-portrait.png`
- root `res/README.txt`

The correct runtime Android assets are already included under:
- `res/mipmap-mdpi/`
- `res/mipmap-hdpi/`
- `res/mipmap-xhdpi/`
- `res/mipmap-xxhdpi/`
- `res/mipmap-xxxhdpi/`
- `res/drawable-nodpi/`

Master/reference brand files are kept outside Android resources under `/brand-assets/`.
