package app.thdev.glassnavlab.core.navigation.runtime

fun interface Router {
    fun navigate(command: RouteCommand)
}
