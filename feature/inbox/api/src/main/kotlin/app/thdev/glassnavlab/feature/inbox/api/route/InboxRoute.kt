package app.thdev.glassnavlab.feature.inbox.api.route

import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidTopLevelRoute

object InboxRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/inbox"
    override val selectedDestinationId: String = NotmidDestinationIds.INBOX
    override val title: String = "Inbox"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.INBOX)
}
