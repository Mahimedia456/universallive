package com.universallive.app.navigation

enum class AppDestination(
    val label: String,
    val showInBottomNav: Boolean = true,
) {
    Home("Home"),
    Scenes("Studio"),
    GoLive("LIVE"),
    Activity("Activity"),
    Settings("Profile"),
    Overlays("Overlays", showInBottomNav = false),
    Connections("Connections", showInBottomNav = false),
}
