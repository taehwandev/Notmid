package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft as NotmidCaptureDraftModel
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState as NotmidCaptureMediaStateModel
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility as NotmidCaptureVisibilityModel
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus as NotmidChatInviteStatusModel
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship as NotmidChatRelationshipModel
import app.thdev.glassnavlab.core.model.notmid.NotmidClip as NotmidClipModel
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment as NotmidMessageAttachmentModel
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace as NotmidPlaceModel
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage as NotmidThreadMessageModel

fun List<NotmidDestinationModel>.toNotmidDestinations(): List<NotmidDestination> {
    return map { it.toUi() }
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
