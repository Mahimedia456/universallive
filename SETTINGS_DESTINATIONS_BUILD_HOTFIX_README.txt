Universal Live Settings Destinations build hotfix

Fixes orphaned `private set` in MobileIntegrationState.kt that caused the Kotlin syntax error around line 407.

Merge into E:\UniversalLive and run:

cd E:\UniversalLive
.\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
.\gradlew.bat :androidApp:assembleDebug
