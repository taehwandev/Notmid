package app.thdev.glassnavlab.core.navigation.notmid

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack

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
