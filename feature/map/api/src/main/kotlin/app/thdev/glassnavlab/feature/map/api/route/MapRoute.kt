package app.thdev.glassnavlab.feature.map.api.route

import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidTopLevelRoute

object MapRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/map"
    override val selectedDestinationId: String = NotmidDestinationIds.MAP
    override val title: String = "Map"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.MAP)
}
