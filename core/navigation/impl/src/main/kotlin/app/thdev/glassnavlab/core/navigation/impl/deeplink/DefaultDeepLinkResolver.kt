package app.thdev.glassnavlab.core.navigation.impl.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkResolver
import app.thdev.glassnavlab.core.navigation.registry.RouteRegistry
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

class DefaultDeepLinkResolver(
    private val policy: DeepLinkUrlPolicy,
    private val routeRegistry: RouteRegistry,
    private val parser: DeepLinkRequestParser = UriDeepLinkRequestParser(),
) : DeepLinkResolver {
    override fun resolve(uriString: String): RoutePlan? {
        val request = parser.parse(uriString) ?: return null
        val normalizedRequest = policy.normalize(request) ?: return null
        return routeRegistry.resolve(normalizedRequest)
    }
}
