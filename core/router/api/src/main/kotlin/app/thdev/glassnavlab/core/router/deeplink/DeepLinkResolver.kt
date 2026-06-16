package app.thdev.glassnavlab.core.router.deeplink

import app.thdev.glassnavlab.core.router.runtime.RoutePlan

fun interface DeepLinkResolver {
    fun resolve(uriString: String): RoutePlan?
}
