package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent

internal object NotmidRouteEventMapper {
    fun planFor(event: RouteEvent): RoutePlan? {
        return stackFor(event)?.let(RoutePlan::compose)
    }

    fun stackFor(event: RouteEvent): RouteStack? {
        return when (event) {
            is NotmidRouteEvent -> stackForNotmidEvent(event)
            is FeedRouteEvent -> stackForFeedEvent(event)
            is MapRouteEvent -> stackForMapEvent(event)
            is InboxRouteEvent -> stackForInboxEvent(event)
            else -> null
        }
    }

    private fun stackForNotmidEvent(event: NotmidRouteEvent): RouteStack {
        return when (event) {
            is NotmidRouteEvent.DestinationSelected -> {
                RouteStack.single(NotmidRouteGraph.destination(event.destinationId))
            }

            NotmidRouteEvent.SettingsRequested -> {
                NotmidRouteGraph.settingsStack()
            }
        }
    }

    private fun stackForFeedEvent(event: FeedRouteEvent): RouteStack {
        return when (event) {
            is FeedRouteEvent.ClipRequested -> {
                NotmidRouteGraph.clipStack(event.clipId)
            }
        }
    }

    private fun stackForMapEvent(event: MapRouteEvent): RouteStack {
        return when (event) {
            is MapRouteEvent.PlaceRequested -> {
                NotmidRouteGraph.placeStack(event.placeId)
            }
        }
    }

    private fun stackForInboxEvent(event: InboxRouteEvent): RouteStack {
        return when (event) {
            is InboxRouteEvent.ChatThreadRequested -> {
                NotmidRouteGraph.chatThreadStack(event.threadId)
            }
        }
    }
}
