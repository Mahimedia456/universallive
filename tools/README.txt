Universal Live — Final verifier syntax fix

Replace:
E:\UniversalLive\tools\mobile-backend-final-fix-verify.ps1

with:
mobile-backend-final-fix-verify.ps1

Then run:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\UniversalLive\tools\mobile-backend-final-fix-verify.ps1"

Production:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\UniversalLive\tools\mobile-backend-final-fix-verify.ps1" -BaseUrl "https://universallive.vercel.app/api/v1"

The verifier treats an empty/null active-session response as a valid "not currently live" state.
