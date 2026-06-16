package app.thdev.glassnavlab.core.router.runtime

import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.core.router.route.ComposeRoute

/**
 * Closed router execution command family.
 *
 * Feature modules should normally emit route events or route intents. The app
 * router converts them into one of these execution commands.
 */
sealed interface RouteCommand {
    val launchSingleTop: Boolean
    val restoreState: Boolean
    val plan: RoutePlan

    data class SetComposeStack(
        val stack: RouteStack,
        override val launchSingleTop: Boolean = true,
        override val restoreState: Boolean = true,
    ) : RouteCommand {
        val route: ComposeRoute
            get() = stack.topRoute

        override val plan: RoutePlan
            get() = RoutePlan.compose(
                stack = stack,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
    }

    data class LaunchActivity(
        val route: ActivityRoute,
        override val launchSingleTop: Boolean = true,
        override val restoreState: Boolean = true,
    ) : RouteCommand {
        override val plan: RoutePlan
            get() = RoutePlan.activity(
                route = route,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
    }

    companion object {
        operator fun invoke(
            stack: RouteStack,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): SetComposeStack {
            return SetComposeStack(
                stack = stack,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        operator fun invoke(
            route: ComposeRoute,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): SetComposeStack {
            return SetComposeStack(
                stack = RouteStack.single(route),
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        operator fun invoke(
            route: ActivityRoute,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): LaunchActivity {
            return LaunchActivity(
                route = route,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }
    }
}
