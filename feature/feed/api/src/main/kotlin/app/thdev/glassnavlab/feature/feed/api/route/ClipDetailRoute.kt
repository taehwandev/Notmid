package app.thdev.glassnavlab.feature.feed.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

data class ClipDetailRoute(
    val clipId: String,
) : NotmidRoute {
    init {
        require(clipId.isNotBlank()) { "clipId must not be blank." }
    }

    override val route: String = "notmid/clips/$clipId"
    override val selectedDestinationId: String = NotmidDestinationIds.FEED
    override val title: String = "Clip"
    override val deepLinkPathSegments: List<String> = listOf("clips", clipId)
}
