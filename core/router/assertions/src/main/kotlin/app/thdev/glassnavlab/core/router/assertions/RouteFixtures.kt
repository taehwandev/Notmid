package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.core.router.route.ComposeRoute
import app.thdev.glassnavlab.core.router.runtime.RouteEvent

data class TestComposeRoute(
    override val route: String,
) : ComposeRoute

data class TestActivityRoute(
    override val route: String,
    override val activityKey: String,
) : ActivityRoute

data class TestRouteEvent(
    val name: String,
) : RouteEvent
