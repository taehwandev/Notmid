package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ActivityRoute

data class TestActivityRoute(
    override val route: String,
    override val activityKey: String,
) : ActivityRoute
