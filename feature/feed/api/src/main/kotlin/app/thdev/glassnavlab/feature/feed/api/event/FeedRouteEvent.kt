package app.thdev.glassnavlab.feature.feed.api.event

import app.thdev.glassnavlab.core.router.runtime.RouteEvent

sealed interface FeedRouteEvent : RouteEvent {
    data class ClipRequested(
        val clipId: String,
    ) : FeedRouteEvent
}
