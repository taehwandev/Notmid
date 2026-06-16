package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.core.router.route.ComposeRoute
import app.thdev.glassnavlab.core.router.route.TopLevelRoute
import app.thdev.glassnavlab.core.router.runtime.RouteEvent

data class TestComposeRoute(
    override val route: String,
) : ComposeRoute

data class TestTopLevelRoute(
    override val route: String,
    override val destinationId: String = route,
    override val title: String = route,
) : TopLevelRoute

data class TestActivityRoute(
    override val route: String,
    override val activityKey: String,
) : ActivityRoute

data class TestRouteEvent(
    val name: String,
) : RouteEvent
