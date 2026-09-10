UNIVERSAL LIVE — MOBILE + BACKEND CONNECTION / GO LIVE FIX PACKAGE

Scope
- Mobile + backend only.
- New backend-connected destination screens are the active connection/live setup flow.
- Legacy duplicate connection/go-live screens were removed from this package.

Key fixes
- Mobile broadcast API aligned to /streams/sessions backend routes.
- Authenticated RTMP publish-config flow used for Go Live.
- Destination readiness now requires enabled connection + configured credentials.
- Membership limits are enforced by backend broadcast authorization.
- Connections support Add / Manage / Edit / Delete / Test / Use for Live flow.
- Live setup can add a destination or open Manage Connections directly.
- Primary cyan/blue buttons use white content text.
- Visible Cloud wording in the active Studio/account flow changed to Backend/account-synced terminology.

Cleanup script
- mobile/tools/cleanup-unused-legacy.ps1
- Default is DRY RUN.
- Delete listed legacy/build targets only with:
    .\tools\cleanup-unused-legacy.ps1 -Apply
- Add -IncludeBackups only if you also want backend *.bak files removed.

Security
- Backend .env is intentionally excluded from the deliverable ZIP.
- Use backend/.env.example and your own local/deployment secrets.
