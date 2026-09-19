package app.thdev.glassnavlab.feature.inbox.api.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds

object ChatDeepLinkSpec : DeepLinkSpec {
    override val priority: Int = 20

    override fun match(request: DeepLinkRequest): RoutePlan? {
        val threadId = when {
            request.pathSegments.size == 2 && request.pathSegments[0] == CHATS_PATH -> {
                request.pathSegments[1]
            }

            request.pathSegments.size == 3 &&
                request.pathSegments[0] == NotmidDestinationIds.INBOX &&
                request.pathSegments[1] == CHATS_PATH -> {
                request.pathSegments[2]
            }

            else -> return null
        }.takeIf(String::isNotBlank) ?: return null

        return RoutePlan.compose(
            RouteStack.of(
                InboxRoute,
                ChatThreadRoute(threadId),
            ),
        )
    }
}

private const val CHATS_PATH = "chats"
