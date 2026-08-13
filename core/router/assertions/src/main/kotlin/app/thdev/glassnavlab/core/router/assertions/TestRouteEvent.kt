package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.runtime.RouteEvent

data class TestRouteEvent(
    val name: String,
) : RouteEvent
