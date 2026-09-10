# iOS host

The shared Compose UI framework is produced by `composeApp` as `ComposeApp`.
A runnable iOS host project will be added in the dedicated iOS phase because Xcode project generation/build/signing requires macOS + Xcode.

The shared iOS entry point already exists at:
`composeApp/src/iosMain/kotlin/com/universallive/app/MainViewController.kt`
