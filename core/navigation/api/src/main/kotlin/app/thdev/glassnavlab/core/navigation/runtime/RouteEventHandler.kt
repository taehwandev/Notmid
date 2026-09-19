package app.thdev.glassnavlab.core.navigation.runtime

fun interface RouteEventHandler {
    fun planFor(event: RouteEvent): RoutePlan?
}
