package app.thdev.glassnavlab.core.router.impl.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.route.ComposeRoute
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack

class StaticRouteDeepLinkSpec(
    private val pathSegments: List<String>,
    private val planFactory: () -> RoutePlan,
    override val priority: Int = 0,
) : DeepLinkSpec {
    constructor(
        pathSegments: List<String>,
        route: ComposeRoute,
        priority: Int = 0,
    ) : this(
        pathSegments = pathSegments,
        planFactory = { RoutePlan.compose(RouteStack.single(route)) },
        priority = priority,
    )

    init {
        require(pathSegments.isNotEmpty()) {
            "Static route deep-link path must contain at least one segment."
        }
        require(pathSegments.none(String::isBlank)) {
            "Static route deep-link path must not contain blank segments."
        }
    }

    override fun match(request: DeepLinkRequest): RoutePlan? {
        if (request.pathSegments != pathSegments) return null
        return planFactory()
    }
}
