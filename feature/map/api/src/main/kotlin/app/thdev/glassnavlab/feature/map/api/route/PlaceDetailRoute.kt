package app.thdev.glassnavlab.feature.map.api.route

import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute

data class PlaceDetailRoute(
    val placeId: String,
) : NotmidRoute {
    init {
        require(placeId.isNotBlank()) { "placeId must not be blank." }
    }

    override val route: String = "notmid/places/$placeId"
    override val selectedDestinationId: String = NotmidDestinationIds.MAP
    override val title: String = "Place"
    override val deepLinkPathSegments: List<String> = listOf("places", placeId)
}
