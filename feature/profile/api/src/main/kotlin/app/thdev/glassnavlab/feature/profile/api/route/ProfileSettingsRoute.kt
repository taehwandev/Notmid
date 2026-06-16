package app.thdev.glassnavlab.feature.profile.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

object ProfileSettingsRoute : NotmidRoute {
    override val route: String = "notmid/profile/settings"
    override val selectedDestinationId: String = NotmidDestinationIds.PROFILE
    override val title: String = "Settings"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.SETTINGS)
}
