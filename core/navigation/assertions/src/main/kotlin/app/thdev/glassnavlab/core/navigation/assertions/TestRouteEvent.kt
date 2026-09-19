package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent

data class TestRouteEvent(
    val name: String,
) : RouteEvent
