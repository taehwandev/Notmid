package app.thdev.glassnavlab.core.model.notmid

data class NotmidCapturePublishRequest(
    val draftId: String,
    val caption: String,
    val placeId: String,
    val moodTags: List<String>,
    val visibility: NotmidCaptureVisibility,
)

enum class NotmidCaptureModerationStatus {
    Queued,
    Published,
}

data class NotmidCapturePublishReceipt(
    val clip: NotmidClip,
    val moderationStatus: NotmidCaptureModerationStatus,
)

data class NotmidClipSaveReceipt(
    val clip: NotmidClip,
    val saved: Boolean,
)

sealed interface NotmidMessageAttachment {
    data class Clip(val clipId: String) : NotmidMessageAttachment
    data class Place(val placeId: String) : NotmidMessageAttachment
    data class Route(
        val title: String,
        val placeIds: List<String>,
    ) : NotmidMessageAttachment
}

data class NotmidThreadMessage(
    val id: String,
    val threadId: String,
    val senderHandle: String,
    val body: String,
    val createdAtLabel: String,
    val mine: Boolean,
    val attachment: NotmidMessageAttachment? = null,
)

data class NotmidSendThreadMessageRequest(
    val body: String,
    val attachment: NotmidMessageAttachment? = null,
)

data class NotmidSendThreadMessageReceipt(
    val message: NotmidThreadMessage,
)

data class NotmidStartThreadRequest(
    val participantHandle: String,
    val body: String,
    val attachedClipId: String? = null,
    val attachedPlaceId: String? = null,
)

data class NotmidStartThreadReceipt(
    val thread: NotmidThread,
    val message: NotmidThreadMessage? = null,
)

enum class NotmidChatInviteDecision {
    Accept,
    Reject,
}

data class NotmidChatInviteResponseReceipt(
    val thread: NotmidThread,
)

data class NotmidProfilePrivacySettings(
    val savedPlacesVisibility: String,
    val chatInvites: String,
    val defaultReceiptVisibility: NotmidCaptureVisibility,
)

data class NotmidProfileSettings(
    val user: NotmidAuthUser,
    val privacy: NotmidProfilePrivacySettings,
)

data class NotmidProfileSettingsUpdateRequest(
    val displayName: String,
    val homeNeighborhood: String,
)

data class NotmidProfileSettingsUpdateReceipt(
    val settings: NotmidProfileSettings,
    val updated: Boolean,
)
