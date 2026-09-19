package app.thdev.glassnavlab.core.runtime.router.planner

import app.thdev.glassnavlab.core.navigation.runtime.RouteCommand
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

interface AppRoutePlanner {
    fun planFor(command: RouteCommand): RoutePlan
    fun planFor(event: RouteEvent): RoutePlan?
    fun planForDeepLink(uriString: String): RoutePlan?
}
