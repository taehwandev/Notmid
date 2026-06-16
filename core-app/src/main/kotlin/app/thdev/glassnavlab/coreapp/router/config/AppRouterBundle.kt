package app.thdev.glassnavlab.coreapp.router.config

import app.thdev.glassnavlab.core.router.registry.RouteRegistry
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.coreapp.router.planner.AppRoutePlanner
import app.thdev.glassnavlab.coreapp.router.runtime.AppRouterRuntime

interface AppRouterBundle {
    val registry: RouteRegistry
    val initialStack: RouteStack
    val routePlanner: AppRoutePlanner

    fun createRuntime(): AppRouterRuntime
}
