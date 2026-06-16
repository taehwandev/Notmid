package app.thdev.glassnavlab.core.router.deeplink

import app.thdev.glassnavlab.core.router.runtime.RoutePlan

interface DeepLinkSpec {
    val priority: Int
        get() = 0

    fun match(request: DeepLinkRequest): RoutePlan?
}
