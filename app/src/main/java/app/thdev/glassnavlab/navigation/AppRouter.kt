package app.thdev.glassnavlab.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.thdev.glassnavlab.core.router.ActivityRoute
import app.thdev.glassnavlab.core.router.Route
import app.thdev.glassnavlab.core.router.RouteCommand
import app.thdev.glassnavlab.core.router.RouteEvent
import app.thdev.glassnavlab.core.router.RouteEventSink
import app.thdev.glassnavlab.core.router.RoutePlan
import app.thdev.glassnavlab.core.router.RouteStack
import app.thdev.glassnavlab.core.router.Router
import app.thdev.glassnavlab.feature.notmid.api.NotmidRoute

@Composable
internal fun rememberAppRouter(): AppRouter {
    return remember { AppRouter() }
}

@Stable
internal class AppRouter(
    private val routeCoordinator: AppRouteCoordinator = AppRouteCoordinator(),
) : Router, RouteEventSink {
    var backStack: RouteStack by mutableStateOf(
        RouteStack.single(NotmidRouteGraph.defaultRoute),
    )
        private set

    val currentRoute: Route
        get() = backStack.topRoute

    val notmidRouteStack: List<NotmidRoute>
        get() = backStack.entries.filterIsInstance<NotmidRoute>()

    val selectedNotmidDestinationId: String?
        get() = notmidRouteStack.lastOrNull()?.selectedDestinationId

    private var pendingActivityRouteRequests: List<ActivityRouteRequest> by mutableStateOf(emptyList())

    private var nextActivityRouteRequestId = 0L

    val pendingActivityRouteRequest: ActivityRouteRequest?
        get() = pendingActivityRouteRequests.firstOrNull()

    override fun navigate(command: RouteCommand) {
        execute(routeCoordinator.planFor(command))
    }

    fun navigateDeepLink(uriString: String) {
        routeCoordinator.planForDeepLink(uriString)?.let(::execute)
    }

    fun execute(plan: RoutePlan) {
        plan.composeStack?.let { stack ->
            backStack = stack
        }

        if (plan.activityRoutes.isNotEmpty()) {
            pendingActivityRouteRequests = pendingActivityRouteRequests +
                plan.activityRoutes.map { route ->
                    ActivityRouteRequest(
                        id = ++nextActivityRouteRequestId,
                        route = route,
                    )
                }
        }
    }

    fun consumeActivityRouteRequest(id: Long) {
        pendingActivityRouteRequests = pendingActivityRouteRequests
            .filterNot { request -> request.id == id }
    }

    override fun onRouteEvent(event: RouteEvent) {
        routeCoordinator.planFor(event)?.let(::execute)
    }
}

internal data class ActivityRouteRequest(
    val id: Long,
    val route: ActivityRoute,
)
