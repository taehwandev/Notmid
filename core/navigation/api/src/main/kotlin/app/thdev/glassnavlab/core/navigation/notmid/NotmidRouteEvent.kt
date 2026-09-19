package app.thdev.glassnavlab.core.navigation.notmid

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent

sealed interface NotmidRouteEvent : RouteEvent {
    data class DestinationSelected(
        val destinationId: String,
    ) : NotmidRouteEvent

    object SettingsRequested : NotmidRouteEvent
}
