package app.thdev.glassnavlab.core.navigation.deeplink

import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

fun interface DeepLinkResolver {
    fun resolve(uriString: String): RoutePlan?
}
