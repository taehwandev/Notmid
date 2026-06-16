package app.thdev.glassnavlab.feature.feed.api.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds

object ClipDeepLinkSpec : DeepLinkSpec {
    override val priority: Int = 20

    override fun match(request: DeepLinkRequest): RoutePlan? {
        val clipId = when {
            request.pathSegments.size == 2 && request.pathSegments[0] == CLIPS_PATH -> {
                request.pathSegments[1]
            }

            request.pathSegments.size == 3 &&
                request.pathSegments[0] == NotmidDestinationIds.FEED &&
                request.pathSegments[1] == CLIPS_PATH -> {
                request.pathSegments[2]
            }

            else -> return null
        }.takeIf(String::isNotBlank) ?: return null

        return RoutePlan.compose(
            RouteStack.of(
                FeedRoute,
                ClipDetailRoute(clipId),
            ),
        )
    }
}

private const val CLIPS_PATH = "clips"
