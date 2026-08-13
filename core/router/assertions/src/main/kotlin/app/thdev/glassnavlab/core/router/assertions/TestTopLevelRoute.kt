package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.TopLevelRoute

data class TestTopLevelRoute(
    override val route: String,
    override val destinationId: String = route,
    override val title: String = route,
) : TopLevelRoute
