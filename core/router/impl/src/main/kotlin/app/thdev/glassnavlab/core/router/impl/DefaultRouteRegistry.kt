package app.thdev.glassnavlab.core.router.impl

import app.thdev.glassnavlab.core.router.DeepLinkSpec
import app.thdev.glassnavlab.core.router.Route
import app.thdev.glassnavlab.core.router.RouteRegistry
import app.thdev.glassnavlab.core.router.RouteStack
import app.thdev.glassnavlab.core.router.TopLevelRouteSpec
import app.thdev.glassnavlab.core.router.WebRoute
import app.thdev.glassnavlab.core.router.WebRouteLink

class DefaultRouteRegistry(
    defaultRoute: Route,
    override val topLevelRouteSpecs: List<TopLevelRouteSpec<*>>,
    override val deepLinkSpecs: List<DeepLinkSpec>,
) : RouteRegistry {
    override val defaultStack: RouteStack = RouteStack.single(defaultRoute)

    override fun stackForDestination(destinationId: String): RouteStack? {
        return topLevelRouteSpecs
            .firstOrNull { it.destinationId == destinationId }
            ?.route
            ?.let(RouteStack::single)
    }

    override fun resolve(link: WebRouteLink): RouteStack? {
        if (link.pathSegments.isEmpty()) return defaultStack

        return deepLinkSpecs
            .sortedByDescending(DeepLinkSpec::priority)
            .firstNotNullOfOrNull { spec -> spec.match(link) }
    }
}

class StaticRouteDeepLinkSpec(
    private val route: WebRoute,
    private val stackFactory: () -> RouteStack = { RouteStack.single(route) },
    override val priority: Int = 0,
) : DeepLinkSpec {
    override fun match(link: WebRouteLink): RouteStack? {
        if (link.pathSegments != route.webPathSegments) return null
        return stackFactory()
    }
}

class PrefixRouteDeepLinkSpec(
    private val pathPrefix: List<String>,
    private val stackFactory: (remainingPathSegments: List<String>) -> RouteStack?,
    override val priority: Int = 0,
) : DeepLinkSpec {
    override fun match(link: WebRouteLink): RouteStack? {
        if (link.pathSegments.size < pathPrefix.size) return null
        if (link.pathSegments.take(pathPrefix.size) != pathPrefix) return null
        return stackFactory(link.pathSegments.drop(pathPrefix.size))
    }
}
