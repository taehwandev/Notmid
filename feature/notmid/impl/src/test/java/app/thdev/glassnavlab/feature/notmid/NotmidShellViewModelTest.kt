package app.thdev.glassnavlab.feature.notmid

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class NotmidShellViewModelTest {
    @Test fun userActionsProduceRoutesThroughInjectedPort() {
        val events = mutableListOf<RouteEvent>()
        val vm = NotmidShellViewModel(RouteEventSink { events.add(it) })
        vm.onAction(NotmidShellAction.DestinationClicked("map"))
        vm.onAction(NotmidShellAction.ThreadClicked("thread-1"))
        vm.onAction(NotmidShellAction.PlaceClicked("place-1"))
        vm.onAction(NotmidShellAction.SettingsClicked)
        assertEquals(listOf(
            NotmidRouteEvent.DestinationSelected("map"),
            InboxRouteEvent.ChatThreadRequested("thread-1"),
            MapRouteEvent.PlaceRequested("place-1"),
            NotmidRouteEvent.SettingsRequested,
        ), events)
    }
}
