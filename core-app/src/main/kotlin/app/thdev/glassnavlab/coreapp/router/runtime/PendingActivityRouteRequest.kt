package app.thdev.glassnavlab.coreapp.router.runtime

import app.thdev.glassnavlab.core.router.route.ActivityRoute

data class PendingActivityRouteRequest(
    val id: Long,
    val route: ActivityRoute,
)
