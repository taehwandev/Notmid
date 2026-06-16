package app.thdev.glassnavlab.core.router.runtime

fun interface Router {
    fun navigate(command: RouteCommand)
}
