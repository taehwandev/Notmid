package app.thdev.glassnavlab.feature.profile.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidTopLevelRoute

object ProfileRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/profile"
    override val selectedDestinationId: String = NotmidDestinationIds.PROFILE
    override val title: String = "Profile"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.PROFILE)
}
