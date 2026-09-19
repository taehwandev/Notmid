package app.thdev.glassnavlab.core.navigation.notmid

import app.thdev.glassnavlab.core.navigation.route.ComposeRoute

interface NotmidRoute : ComposeRoute {
    val selectedDestinationId: String
    val title: String
    val deepLinkPathSegments: List<String>
}
