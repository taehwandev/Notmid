package app.thdev.glassnavlab.core.model.notmid

@JvmInline
value class NotmidColor(val argb: Long)

enum class NotmidNavigationIcon {
    Feed,
    Map,
    Capture,
    Inbox,
    Profile,
}

data class NotmidDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: NotmidNavigationIcon,
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
    val threads: List<NotmidThread> = emptyList(),
    val captureDraft: NotmidCaptureDraft? = null,
    val threadMessages: List<NotmidThreadMessage> = emptyList(),
)

data class NotmidClip(
    val title: String,
    val description: String,
    val badge: String,
    val palette: List<NotmidColor>,
    val isLive: Boolean = false,
    val id: String = title.toStableRouteId(),
    val placeId: String? = null,
    val creatorHandle: String = "",
    val moodTags: List<String> = emptyList(),
    val capturedAtLabel: String = "",
    val qualityLabel: String = "HD",
    val playbackProgress: Float = 0f,
)

data class NotmidPlace(
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<NotmidColor>,
    val heightDp: Int,
    val contentColor: NotmidColor = NotmidColors.White,
    val id: String = title.toStableRouteId(),
    val category: String = "",
    val address: String = "",
    val coordinate: NotmidGeoPoint? = null,
    val openNow: Boolean = true,
    val receiptCount: Int = 0,
)

data class NotmidGeoPoint(
    val latitude: Double,
    val longitude: Double,
)

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

enum class NotmidChatRelationship {
    Friend,
    NonFriend,
}

enum class NotmidChatInviteStatus {
    Accepted,
    PendingInbound,
    PendingOutbound,
    Rejected,
}

data class NotmidChatAccess(
    val relationship: NotmidChatRelationship,
    val inviteStatus: NotmidChatInviteStatus,
    val canSendMessage: Boolean,
    val canAcceptInvite: Boolean,
    val canRejectInvite: Boolean,
    val reasonLabel: String,
) {
    companion object {
        val AcceptedFriend = NotmidChatAccess(
            relationship = NotmidChatRelationship.Friend,
            inviteStatus = NotmidChatInviteStatus.Accepted,
            canSendMessage = true,
            canAcceptInvite = false,
            canRejectInvite = false,
            reasonLabel = "Friends can chat immediately.",
        )
    }
}

enum class NotmidCaptureVisibility {
    Public,
    Friends,
    Private,
}

enum class NotmidCaptureMediaState {
    Empty,
    LocalPreview,
    Uploaded,
}

data class NotmidCaptureDraft(
    val id: String,
    val caption: String,
    val placeId: String?,
    val moodTags: List<String>,
    val visibility: NotmidCaptureVisibility,
    val mediaState: NotmidCaptureMediaState,
    val statusLabel: String,
    val waitTimeLabel: String,
    val crowdLabel: String,
    val priceTierLabel: String,
)

object NotmidColors {
    val White = NotmidColor(0xFFFFFFFF)
    val DarkCardContent = NotmidColor(0xFF17202A)
}

private fun String.toStableRouteId(): String {
    return trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
}
