package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteEventSink

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
