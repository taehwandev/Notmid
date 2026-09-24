package app.thdev.glassnavlab.router.config

import app.thdev.glassnavlab.core.navigation.registry.RouteRegistry
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.router.planner.AppRoutePlanner
import app.thdev.glassnavlab.router.runtime.AppRouterRuntime

interface AppRouterBundle {
    val registry: RouteRegistry
    val initialStack: RouteStack
    val routePlanner: AppRoutePlanner

    fun createRuntime(): AppRouterRuntime
}
