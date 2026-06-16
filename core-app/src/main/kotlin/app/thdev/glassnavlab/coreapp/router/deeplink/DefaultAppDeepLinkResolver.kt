package app.thdev.glassnavlab.coreapp.router.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkResolver
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

class DefaultAppDeepLinkResolver(
    private val resolver: DeepLinkResolver,
) : AppDeepLinkResolver {
    override fun resolve(uriString: String): RoutePlan? {
        return resolver.resolve(uriString)
    }
}
