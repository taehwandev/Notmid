package app.thdev.glassnavlab.core.router.runtime

fun interface RouteEventHandler {
    fun planFor(event: RouteEvent): RoutePlan?
}
