package app.thdev.glassnavlab.core.runtime.router.config

import app.thdev.glassnavlab.core.navigation.impl.deeplink.DeepLinkUrlPolicy
import app.thdev.glassnavlab.core.navigation.impl.deeplink.DefaultDeepLinkResolver
import app.thdev.glassnavlab.core.navigation.impl.event.DefaultRouteEventPlanner
import app.thdev.glassnavlab.core.navigation.impl.registry.DefaultRouteRegistry
import app.thdev.glassnavlab.core.navigation.registry.RouteRegistry
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.runtime.router.deeplink.DefaultAppDeepLinkResolver
import app.thdev.glassnavlab.core.runtime.router.planner.AppRoutePlanner
import app.thdev.glassnavlab.core.runtime.router.planner.DefaultAppRoutePlanner
import app.thdev.glassnavlab.core.runtime.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.core.runtime.router.runtime.DefaultAppRouterRuntime

class DefaultAppRouterBundle(
    config: AppRouterBundleConfig,
) : AppRouterBundle {
    override val registry: RouteRegistry = DefaultRouteRegistry(
        defaultRoute = config.defaultRoute,
        topLevelRoutes = config.topLevelRoutes,
        deepLinkSpecs = config.deepLinkSpecs,
    )

    override val initialStack: RouteStack = registry.defaultStack

    override val routePlanner: AppRoutePlanner = DefaultAppRoutePlanner(
        routeEventPlanner = DefaultRouteEventPlanner(config.routeEventHandlers),
        deepLinkResolver = DefaultAppDeepLinkResolver(
            resolver = DefaultDeepLinkResolver(
                policy = config.deepLinkUrlConfig.toDeepLinkUrlPolicy(),
                routeRegistry = registry,
            ),
        ),
    )

    override fun createRuntime(): AppRouterRuntime {
        return DefaultAppRouterRuntime(
            initialStack = initialStack,
            routePlanner = routePlanner,
        )
    }
}

private fun AppDeepLinkUrlConfig.toDeepLinkUrlPolicy(): DeepLinkUrlPolicy {
    return DeepLinkUrlPolicy(
        allowedSchemes = allowedSchemes,
        allowedHosts = allowedHosts,
        basePathSegments = basePathSegments,
    )
}
