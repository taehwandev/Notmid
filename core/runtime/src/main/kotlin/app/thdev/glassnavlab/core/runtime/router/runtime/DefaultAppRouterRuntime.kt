package app.thdev.glassnavlab.core.runtime.router.runtime

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.core.router.route.Route
import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.core.runtime.router.planner.AppRoutePlanner

@Stable
class DefaultAppRouterRuntime(
    initialStack: RouteStack,
    private val routePlanner: AppRoutePlanner,
) : AppRouterRuntime {
    override var backStack: RouteStack by mutableStateOf(initialStack)
        private set

    override val currentRoute: Route
        get() = backStack.topRoute

    private var pendingActivityRouteRequests: List<PendingActivityRouteRequest> by mutableStateOf(
        emptyList(),
    )

    private var nextActivityRouteRequestId = 0L

    override val pendingActivityRouteRequest: PendingActivityRouteRequest?
        get() = pendingActivityRouteRequests.firstOrNull()

    override fun navigate(command: RouteCommand) {
        execute(routePlanner.planFor(command))
    }

    override fun navigateDeepLink(uriString: String) {
        routePlanner.planForDeepLink(uriString)?.let(::execute)
    }

    override fun execute(plan: RoutePlan) {
        plan.composeStack?.let { stack ->
            backStack = stack
        }

        if (plan.activityRoutes.isNotEmpty()) {
            pendingActivityRouteRequests = pendingActivityRouteRequests +
                plan.activityRoutes.map(::nextActivityRouteRequest)
        }
    }

    override fun consumeActivityRouteRequest(id: Long) {
        pendingActivityRouteRequests = pendingActivityRouteRequests
            .filterNot { request -> request.id == id }
    }

    override fun onRouteEvent(event: RouteEvent) {
        routePlanner.planFor(event)?.let(::execute)
    }

    private fun nextActivityRouteRequest(route: ActivityRoute): PendingActivityRouteRequest {
        return PendingActivityRouteRequest(
            id = ++nextActivityRouteRequestId,
            route = route,
        )
    }
}
