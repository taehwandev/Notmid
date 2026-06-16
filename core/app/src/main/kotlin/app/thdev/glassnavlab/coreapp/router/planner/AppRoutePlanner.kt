package app.thdev.glassnavlab.coreapp.router.planner

import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

interface AppRoutePlanner {
    fun planFor(command: RouteCommand): RoutePlan
    fun planFor(event: RouteEvent): RoutePlan?
    fun planForDeepLink(uriString: String): RoutePlan?
}
