package app.thdev.glassnavlab.feature.notmid.api.event

import app.thdev.glassnavlab.core.router.runtime.RouteEvent

sealed interface NotmidRouteEvent : RouteEvent {
    data class DestinationSelected(
        val destinationId: String,
    ) : NotmidRouteEvent

    object SettingsRequested : NotmidRouteEvent
}
