package app.thdev.glassnavlab.core.runtime.router.runtime

import app.thdev.glassnavlab.core.router.route.ActivityRoute

data class PendingActivityRouteRequest(
    val id: Long,
    val route: ActivityRoute,
)
