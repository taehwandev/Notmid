package app.thdev.glassnavlab.core.navigation.deeplink

import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

interface DeepLinkSpec {
    val priority: Int
        get() = 0

    fun match(request: DeepLinkRequest): RoutePlan?
}
