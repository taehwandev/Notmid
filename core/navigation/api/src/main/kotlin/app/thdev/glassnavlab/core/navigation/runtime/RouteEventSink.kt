package app.thdev.glassnavlab.core.navigation.runtime

fun interface RouteEventSink {
    fun onRouteEvent(event: RouteEvent)
}
