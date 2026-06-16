package app.thdev.glassnavlab.core.router.route

interface TopLevelRoute : ComposeRoute {
    val destinationId: String
    val title: String
}
