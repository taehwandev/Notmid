package app.thdev.glassnavlab.feature.notmid.api.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

open class NotmidStaticDeepLinkSpec(
    private val route: NotmidRoute,
    private val planFactory: () -> RoutePlan = { RoutePlan.compose(RouteStack.single(route)) },
    override val priority: Int = 0,
) : DeepLinkSpec {
    override fun match(request: DeepLinkRequest): RoutePlan? {
        if (request.pathSegments != route.deepLinkPathSegments) return null
        return planFactory()
    }
}
