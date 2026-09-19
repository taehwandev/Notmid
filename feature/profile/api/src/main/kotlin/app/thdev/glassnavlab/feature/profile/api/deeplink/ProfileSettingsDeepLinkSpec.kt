package app.thdev.glassnavlab.feature.profile.api.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute

object ProfileSettingsDeepLinkSpec : DeepLinkSpec {
    override val priority: Int = 10

    override fun match(request: DeepLinkRequest): RoutePlan? {
        if (request.pathSegments != listOf(NotmidDestinationIds.PROFILE, NotmidDestinationIds.SETTINGS)) {
            return null
        }

        return RoutePlan.compose(
            RouteStack.of(
                ProfileRoute,
                ProfileSettingsRoute,
            ),
        )
    }
}
