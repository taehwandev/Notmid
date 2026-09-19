package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.route.TopLevelRoute

data class TestTopLevelRoute(
    override val route: String,
    override val destinationId: String = route,
    override val title: String = route,
) : TopLevelRoute
