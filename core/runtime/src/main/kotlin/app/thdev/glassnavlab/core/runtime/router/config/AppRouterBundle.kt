package app.thdev.glassnavlab.core.runtime.router.config

import app.thdev.glassnavlab.core.navigation.registry.RouteRegistry
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.runtime.router.planner.AppRoutePlanner
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime

interface AppRouterBundle {
    val registry: RouteRegistry
    val initialStack: RouteStack
    val routePlanner: AppRoutePlanner

    fun createRuntime(): AppRouterRuntime
}
