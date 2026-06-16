package app.thdev.glassnavlab.core.router.runtime

import app.thdev.glassnavlab.core.router.route.ActivityRoute

/**
 * Resolved route execution plan.
 *
 * Compose destinations live in [composeStack]. Activity-backed destinations are
 * execution requests in [activityRoutes].
 */
data class RoutePlan(
    val composeStack: RouteStack? = null,
    val activityRoutes: List<ActivityRoute> = emptyList(),
    val launchSingleTop: Boolean = true,
    val restoreState: Boolean = true,
) {
    init {
        require(composeStack != null || activityRoutes.isNotEmpty()) {
            "RoutePlan requires a compose stack or at least one activity route."
        }
    }

    companion object {
        fun fromStack(
            stack: RouteStack,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            return compose(
                stack = stack,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        fun compose(
            stack: RouteStack,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            return RoutePlan(
                composeStack = stack,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        fun activity(
            route: ActivityRoute,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            return RoutePlan(
                activityRoutes = listOf(route),
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }
    }
}
