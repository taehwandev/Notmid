package app.thdev.glassnavlab.feature.feed.api.route

import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidTopLevelRoute

object FeedRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/feed"
    override val selectedDestinationId: String = NotmidDestinationIds.FEED
    override val title: String = "Feed"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.FEED)
}
