package app.thdev.glassnavlab.core.runtime.router.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkResolver
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

class DefaultAppDeepLinkResolver(
    private val resolver: DeepLinkResolver,
) : AppDeepLinkResolver {
    override fun resolve(uriString: String): RoutePlan? {
        return resolver.resolve(uriString)
    }
}
