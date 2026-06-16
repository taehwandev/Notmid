package app.thdev.glassnavlab.core.router.runtime

import app.thdev.glassnavlab.core.router.route.ComposeRoute

/**
 * Ordered Compose back stack resolved by the app router.
 *
 * Activity-backed destinations are execution requests and belong in
 * [RoutePlan.activityRoutes], not in this stack.
 */
data class RouteStack(
    val entries: List<ComposeRoute>,
) {
    init {
        require(entries.isNotEmpty()) { "RouteStack requires at least one route." }
    }

    val topRoute: ComposeRoute
        get() = entries.last()

    val composeRoutes: List<ComposeRoute>
        get() = entries

    companion object {
        fun single(route: ComposeRoute): RouteStack {
            return RouteStack(listOf(route))
        }

        fun of(vararg routes: ComposeRoute): RouteStack {
            return RouteStack(routes.toList())
        }
    }
}
