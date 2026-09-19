package app.thdev.glassnavlab.feature.inbox.api.event

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent

sealed interface InboxRouteEvent : RouteEvent {
    data class ChatThreadRequested(
        val threadId: String,
    ) : InboxRouteEvent
}
