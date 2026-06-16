package app.thdev.glassnavlab.feature.notmid.api.route

import app.thdev.glassnavlab.core.router.route.ComposeRoute

interface NotmidRoute : ComposeRoute {
    val selectedDestinationId: String
    val title: String
    val deepLinkPathSegments: List<String>
}
