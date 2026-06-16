package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft as NotmidCaptureDraftModel
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState as NotmidCaptureMediaStateModel
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility as NotmidCaptureVisibilityModel
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus as NotmidChatInviteStatusModel
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship as NotmidChatRelationshipModel
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment as NotmidMessageAttachmentModel
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidClip as NotmidClipModel
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace as NotmidPlaceModel
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage as NotmidThreadMessageModel

data class NotmidDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: NotmidNavigationIcon,
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
    val threads: List<NotmidThread>,
    val captureDraft: NotmidCaptureDraft?,
    val threadMessages: List<NotmidThreadMessage> = emptyList(),
)

sealed interface NotmidBadge {
    object LiveNow : NotmidBadge
    data class Label(val text: String) : NotmidBadge
    object None : NotmidBadge
}

data class NotmidClip(
    val id: String,
    val title: String,
    val description: String,
    val badge: NotmidBadge,
    val palette: List<Color>,
    val isLive: Boolean = false,
    val placeId: String? = null,
    val creatorHandle: String = "",
    val moodTags: List<String> = emptyList(),
    val capturedAtLabel: String = "",
    val qualityLabel: String = "HD",
    val playbackProgress: Float = 0f,
)

data class NotmidPlace(
    val id: String,
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<Color>,
    val height: Dp,
    val contentColor: Color,
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

data class NotmidThreadMessage(
    val id: String,
    val threadId: String,
    val senderHandle: String,
    val body: String,
    val createdAtLabel: String,
    val mine: Boolean,
    val attachment: NotmidThreadMessageAttachment? = null,
)

sealed interface NotmidThreadMessageAttachment {
    data class Clip(val clipId: String) : NotmidThreadMessageAttachment
    data class Place(val placeId: String) : NotmidThreadMessageAttachment
    data class Route(
        val title: String,
        val placeIds: List<String>,
    ) : NotmidThreadMessageAttachment
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

val NotmidBackgroundColor = NotmidColorTokens.WarmMist

fun List<NotmidDestinationModel>.toNotmidDestinations(): List<NotmidDestination> {
    return map { it.toUi() }
}

fun destinationFor(
    destinations: List<NotmidDestination>,
    selectedItemId: String,
): NotmidDestination {
    return destinations.firstOrNull { it.id == selectedItemId } ?: destinations.first()
}

fun backdropPaletteForItem(
    destination: NotmidDestination,
    itemIndex: Int,
): List<Color>? {
    val contentIndex = itemIndex - 1
    if (contentIndex < 0) return null

    return when {
        contentIndex < destination.clips.size -> destination.clips[contentIndex].palette
        else -> {
            val placeIndex = contentIndex - destination.clips.size
            destination.places.getOrNull(placeIndex)?.palette
        }
    }
}

fun notmidPalette(
    palette: List<Color>,
    fraction: Float,
): Color {
    if (palette.isEmpty()) return NotmidBackgroundColor
    if (palette.size == 1) return palette.first()

    val scaledFraction = fraction.coerceIn(0f, 1f) * palette.lastIndex
    val startIndex = scaledFraction.toInt().coerceIn(0, palette.lastIndex - 1)
    val endIndex = startIndex + 1
    return lerpColor(
        start = palette[startIndex],
        stop = palette[endIndex],
        fraction = scaledFraction - startIndex,
    )
}

private fun NotmidDestinationModel.toUi(): NotmidDestination {
    return NotmidDestination(
        id = id,
        title = title,
        subtitle = subtitle,
        icon = icon,
        clips = clips.map(NotmidClipModel::toUi),
        places = places.map(NotmidPlaceModel::toUi),
        threads = threads.map { thread ->
            NotmidThread(
                id = thread.id,
                title = thread.title,
                preview = thread.preview,
                updatedAtLabel = thread.updatedAtLabel,
                participantHandles = thread.participantHandles,
                attachedPlaceId = thread.attachedPlaceId,
                attachedClipId = thread.attachedClipId,
                unreadCount = thread.unreadCount,
                chatAccess = NotmidChatAccess(
                    relationship = thread.chatAccess.relationship.toUi(),
                    inviteStatus = thread.chatAccess.inviteStatus.toUi(),
                    canSendMessage = thread.chatAccess.canSendMessage,
                    canAcceptInvite = thread.chatAccess.canAcceptInvite,
                    canRejectInvite = thread.chatAccess.canRejectInvite,
                    reasonLabel = thread.chatAccess.reasonLabel,
                ),
            )
        },
        captureDraft = captureDraft?.toUi(),
        threadMessages = threadMessages.map(NotmidThreadMessageModel::toUi),
    )
}

private fun NotmidClipModel.toUi(): NotmidClip {
    val uiBadge = when {
        isLive -> NotmidBadge.LiveNow
        badge.trim().isEmpty() -> NotmidBadge.None
        else -> NotmidBadge.Label(badge)
    }
    return NotmidClip(
        id = id,
        title = title,
        description = description,
        badge = uiBadge,
        palette = palette.map(NotmidColor::toColor),
        isLive = isLive,
        placeId = placeId,
        creatorHandle = creatorHandle,
        moodTags = moodTags,
        capturedAtLabel = capturedAtLabel,
        qualityLabel = qualityLabel,
        playbackProgress = playbackProgress.coerceIn(0f, 1f),
    )
}

private fun NotmidPlaceModel.toUi(): NotmidPlace {
    return NotmidPlace(
        id = id,
        title = title,
        description = description,
        metric = metric,
        palette = palette.map(NotmidColor::toColor),
        height = heightDp.dp,
        contentColor = contentColor.toColor(),
        category = category,
        address = address,
        coordinate = coordinate?.let {
            NotmidGeoPoint(
                latitude = it.latitude,
                longitude = it.longitude,
            )
        },
        openNow = openNow,
        receiptCount = receiptCount,
    )
}

private fun NotmidCaptureDraftModel.toUi(): NotmidCaptureDraft {
    return NotmidCaptureDraft(
        id = id,
        caption = caption,
        placeId = placeId,
        moodTags = moodTags,
        visibility = visibility.toUi(),
        mediaState = mediaState.toUi(),
        statusLabel = statusLabel,
        waitTimeLabel = waitTimeLabel,
        crowdLabel = crowdLabel,
        priceTierLabel = priceTierLabel,
    )
}

private fun NotmidThreadMessageModel.toUi(): NotmidThreadMessage {
    return NotmidThreadMessage(
        id = id,
        threadId = threadId,
        senderHandle = senderHandle,
        body = body,
        createdAtLabel = createdAtLabel,
        mine = mine,
        attachment = attachment?.toUi(),
    )
}

private fun NotmidMessageAttachmentModel.toUi(): NotmidThreadMessageAttachment {
    return when (this) {
        is NotmidMessageAttachmentModel.Clip -> NotmidThreadMessageAttachment.Clip(clipId)
        is NotmidMessageAttachmentModel.Place -> NotmidThreadMessageAttachment.Place(placeId)
        is NotmidMessageAttachmentModel.Route -> NotmidThreadMessageAttachment.Route(
            title = title,
            placeIds = placeIds,
        )
    }
}

private fun NotmidCaptureVisibilityModel.toUi(): NotmidCaptureVisibility {
    return when (this) {
        NotmidCaptureVisibilityModel.Public -> NotmidCaptureVisibility.Public
        NotmidCaptureVisibilityModel.Friends -> NotmidCaptureVisibility.Friends
        NotmidCaptureVisibilityModel.Private -> NotmidCaptureVisibility.Private
    }
}

private fun NotmidCaptureMediaStateModel.toUi(): NotmidCaptureMediaState {
    return when (this) {
        NotmidCaptureMediaStateModel.Empty -> NotmidCaptureMediaState.Empty
        NotmidCaptureMediaStateModel.LocalPreview -> NotmidCaptureMediaState.LocalPreview
        NotmidCaptureMediaStateModel.Uploaded -> NotmidCaptureMediaState.Uploaded
    }
}

private fun NotmidChatRelationshipModel.toUi(): NotmidChatRelationship {
    return when (this) {
        NotmidChatRelationshipModel.Friend -> NotmidChatRelationship.Friend
        NotmidChatRelationshipModel.NonFriend -> NotmidChatRelationship.NonFriend
    }
}

private fun NotmidChatInviteStatusModel.toUi(): NotmidChatInviteStatus {
    return when (this) {
        NotmidChatInviteStatusModel.Accepted -> NotmidChatInviteStatus.Accepted
        NotmidChatInviteStatusModel.PendingInbound -> NotmidChatInviteStatus.PendingInbound
        NotmidChatInviteStatusModel.PendingOutbound -> NotmidChatInviteStatus.PendingOutbound
        NotmidChatInviteStatusModel.Rejected -> NotmidChatInviteStatus.Rejected
    }
}

private fun NotmidColor.toColor(): Color = Color(argb)
