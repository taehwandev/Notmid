package app.thdev.glassnavlab.feature.map.api.event

import app.thdev.glassnavlab.core.router.runtime.RouteEvent

sealed interface MapRouteEvent : RouteEvent {
    data class PlaceRequested(
        val placeId: String,
    ) : MapRouteEvent
}
