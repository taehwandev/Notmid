package app.thdev.glassnavlab.core.navigation.route

interface TopLevelRoute : ComposeRoute {
    val destinationId: String
    val title: String
}
