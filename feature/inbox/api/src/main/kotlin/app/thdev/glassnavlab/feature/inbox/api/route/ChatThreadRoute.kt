package app.thdev.glassnavlab.feature.inbox.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute

data class ChatThreadRoute(
    val threadId: String,
) : NotmidRoute {
    init {
        require(threadId.isNotBlank()) { "threadId must not be blank." }
    }

    override val route: String = "notmid/chats/$threadId"
    override val selectedDestinationId: String = NotmidDestinationIds.INBOX
    override val title: String = "Chat"
    override val deepLinkPathSegments: List<String> = listOf("chats", threadId)
}
