package app.thdev.glassnavlab.feature.inbox.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidTopLevelRoute

object InboxRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/inbox"
    override val selectedDestinationId: String = NotmidDestinationIds.INBOX
    override val title: String = "Inbox"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.INBOX)
}
