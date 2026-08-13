package app.thdev.glassnavlab.feature.inbox

import app.thdev.glassnavlab.feature.notmid.common.model.NotmidChatAccess
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

internal data class InboxThreadUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val participants: String,
    val updatedLabel: String,
    val unreadCount: Int,
    val chatAccess: NotmidChatAccess,
    val clip: NotmidClip?,
    val place: NotmidPlace?,
    val routePlan: String,
    val messages: List<ChatMessageUi> = emptyList(),
)
