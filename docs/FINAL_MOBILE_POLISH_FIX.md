# UniversalLive Final Mobile Polish / Android Build Repair

## Android compile failure fixed
The actual Android task failure was:
`:composeApp:compileAndroidMain`

Affected DTOs imported:
`kotlinx.serialization.json.JsonObject`

The current project did not include the serialization-json artifact.

Instead of adding another dependency late in the build, this repair converts the few backend-contract JSON fields to stable typed/shared Kotlin structures:
- typed `PlanEntitlementsDto`
- `Map<String, String>` for flexible metadata/config diagnostics

This removes the unresolved `kotlinx.serialization.json` compile dependency.

## KMP iOS diagnostics
The `iosX64` Compose 1.12.0 dependency-resolution diagnostics are separate from the Android Kotlin compile failure. This patch intentionally does not remove the iOS target.

## Button polish
The old primary button used:
`contentColor = AppBackground`

That caused dark/grey text on the cyan button.

Final contract:
- cyan primary button -> white text
- red critical button -> white text
- dark secondary button -> white text
- disabled primary button -> white text at reduced opacity
- cyan remains the action/accent color
- red remains LIVE/critical/destructive

## Destination screen
Free plan continues to allow:
- connecting multiple channels
- editing connections
- testing connections
- removing/disconnecting connections

Only simultaneous LIVE output is limited by entitlement.
