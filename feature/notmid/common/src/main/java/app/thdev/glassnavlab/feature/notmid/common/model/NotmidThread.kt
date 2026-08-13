package app.thdev.glassnavlab.feature.notmid.common.model

data class NotmidThread(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAtLabel: String,
    val participantHandles: List<String>,
    val attachedPlaceId: String? = null,
    val attachedClipId: String? = null,
    val unreadCount: Int = 0,
    val chatAccess: NotmidChatAccess = NotmidChatAccess.AcceptedFriend,
)
