package app.thdev.glassnavlab.feature.inbox.api.event

import app.thdev.glassnavlab.core.router.runtime.RouteEvent

sealed interface InboxRouteEvent : RouteEvent {
    data class ChatThreadRequested(
        val threadId: String,
    ) : InboxRouteEvent
}
