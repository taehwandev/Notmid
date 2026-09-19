package app.thdev.glassnavlab.core.navigation.runtime

fun interface RouteEventPlanner {
    fun planFor(event: RouteEvent): RoutePlan?
}
