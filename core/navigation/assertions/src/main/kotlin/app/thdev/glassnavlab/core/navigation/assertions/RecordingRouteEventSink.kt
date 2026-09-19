package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink

class RecordingRouteEventSink : RouteEventSink {
    private val recordedEvents = mutableListOf<RouteEvent>()

    val events: List<RouteEvent>
        get() = recordedEvents.toList()

    val lastEvent: RouteEvent?
        get() = recordedEvents.lastOrNull()

    override fun onRouteEvent(event: RouteEvent) {
        recordedEvents += event
    }

    fun clear() {
        recordedEvents.clear()
    }

    fun assertEvents(vararg expected: RouteEvent) {
        assertEquals(
            expected = expected.toList(),
            actual = events,
            label = "route events",
        )
    }

    fun assertLastEvent(expected: RouteEvent) {
        assertEquals(
            expected = expected,
            actual = lastEvent,
            label = "last route event",
        )
    }
}
