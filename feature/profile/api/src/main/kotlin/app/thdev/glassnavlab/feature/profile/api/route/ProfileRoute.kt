package app.thdev.glassnavlab.feature.profile.api.route

import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidTopLevelRoute

object ProfileRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/profile"
    override val selectedDestinationId: String = NotmidDestinationIds.PROFILE
    override val title: String = "Profile"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.PROFILE)
}
