package app.thdev.glassnavlab.feature.notmid.router

import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.feed.api.event.FeedRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.notmid.api.event.NotmidRouteEvent

internal object NotmidRouteEventMapper {
    val handlers: List<RouteEventHandler> = listOf(
        RouteEventHandler { event ->
            (event as? NotmidRouteEvent)?.let(::planForNotmidEvent)
        },
        RouteEventHandler { event ->
            (event as? FeedRouteEvent)?.let(::planForFeedEvent)
        },
        RouteEventHandler { event ->
            (event as? MapRouteEvent)?.let(::planForMapEvent)
        },
        RouteEventHandler { event ->
            (event as? InboxRouteEvent)?.let(::planForInboxEvent)
        },
    )

    private fun planForNotmidEvent(event: NotmidRouteEvent): RoutePlan {
        return when (event) {
            is NotmidRouteEvent.DestinationSelected -> {
                RouteStack.single(NotmidRouteGraph.destination(event.destinationId))
            }

            NotmidRouteEvent.SettingsRequested -> {
                NotmidRouteGraph.settingsStack()
            }
        }.let(RoutePlan::compose)
    }

    private fun planForFeedEvent(event: FeedRouteEvent): RoutePlan {
        return when (event) {
            is FeedRouteEvent.ClipRequested -> {
                NotmidRouteGraph.clipStack(event.clipId)
            }
        }.let(RoutePlan::compose)
    }

    private fun planForMapEvent(event: MapRouteEvent): RoutePlan {
        return when (event) {
            is MapRouteEvent.PlaceRequested -> {
                NotmidRouteGraph.placeStack(event.placeId)
            }
        }.let(RoutePlan::compose)
    }

    private fun planForInboxEvent(event: InboxRouteEvent): RoutePlan {
        return when (event) {
            is InboxRouteEvent.ChatThreadRequested -> {
                NotmidRouteGraph.chatThreadStack(event.threadId)
            }
        }.let(RoutePlan::compose)
    }
}
