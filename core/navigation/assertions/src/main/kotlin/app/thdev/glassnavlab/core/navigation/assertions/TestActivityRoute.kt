package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.route.ActivityRoute

data class TestActivityRoute(
    override val route: String,
    override val activityKey: String,
) : ActivityRoute
