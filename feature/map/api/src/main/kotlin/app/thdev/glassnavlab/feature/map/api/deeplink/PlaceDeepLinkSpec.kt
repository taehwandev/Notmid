package app.thdev.glassnavlab.feature.map.api.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds

object PlaceDeepLinkSpec : DeepLinkSpec {
    override val priority: Int = 20

    override fun match(request: DeepLinkRequest): RoutePlan? {
        val placeId = when {
            request.pathSegments.size == 2 && request.pathSegments[0] == PLACES_PATH -> {
                request.pathSegments[1]
            }

            request.pathSegments.size == 3 &&
                request.pathSegments[0] == NotmidDestinationIds.MAP &&
                request.pathSegments[1] == PLACES_PATH -> {
                request.pathSegments[2]
            }

            else -> return null
        }.takeIf(String::isNotBlank) ?: return null

        return RoutePlan.compose(
            RouteStack.of(
                MapRoute,
                PlaceDetailRoute(placeId),
            ),
        )
    }
}

private const val PLACES_PATH = "places"
