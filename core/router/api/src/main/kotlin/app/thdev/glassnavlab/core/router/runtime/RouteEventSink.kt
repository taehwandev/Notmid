package app.thdev.glassnavlab.core.router.runtime

fun interface RouteEventSink {
    fun onRouteEvent(event: RouteEvent)
}
