package app.thdev.glassnavlab.core.router.runtime

fun interface RouteEventPlanner {
    fun planFor(event: RouteEvent): RoutePlan?
}
