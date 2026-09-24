package app.thdev.glassnavlab.core.activity.route

import app.thdev.glassnavlab.core.navigation.route.ActivityRoute

data class PendingActivityRouteRequest(
    val id: Long,
    val route: ActivityRoute,
)
