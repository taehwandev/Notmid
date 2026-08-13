package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship

/**
 * Translation between notmid API enum strings and the core model enums.
 *
 * An unknown wire value throws so the caller reports a malformed response
 * instead of silently degrading to a default.
 */
internal fun String.toCaptureVisibility(): NotmidCaptureVisibility {
    return when (this) {
        "public" -> NotmidCaptureVisibility.Public
        "friends" -> NotmidCaptureVisibility.Friends
        "private" -> NotmidCaptureVisibility.Private
        else -> error("Unsupported capture visibility: $this")
    }
}

internal fun NotmidCaptureVisibility.toApiValue(): String {
    return when (this) {
        NotmidCaptureVisibility.Public -> "public"
        NotmidCaptureVisibility.Friends -> "friends"
        NotmidCaptureVisibility.Private -> "private"
    }
}

internal fun String.toCaptureMediaState(): NotmidCaptureMediaState {
    return when (this) {
        "empty" -> NotmidCaptureMediaState.Empty
        "local-preview" -> NotmidCaptureMediaState.LocalPreview
        "uploaded" -> NotmidCaptureMediaState.Uploaded
        else -> error("Unsupported capture media state: $this")
    }
}

internal fun String.toModerationStatus(): NotmidCaptureModerationStatus {
    return when (this) {
        "queued" -> NotmidCaptureModerationStatus.Queued
        "published" -> NotmidCaptureModerationStatus.Published
        else -> error("Unsupported moderation status: $this")
    }
}

internal fun String.toChatRelationship(): NotmidChatRelationship {
    return when (this) {
        "friend" -> NotmidChatRelationship.Friend
        "non-friend" -> NotmidChatRelationship.NonFriend
        else -> error("Unsupported chat relationship: $this")
    }
}

internal fun String.toChatInviteStatus(): NotmidChatInviteStatus {
    return when (this) {
        "accepted" -> NotmidChatInviteStatus.Accepted
        "pending-inbound" -> NotmidChatInviteStatus.PendingInbound
        "pending-outbound" -> NotmidChatInviteStatus.PendingOutbound
        "rejected" -> NotmidChatInviteStatus.Rejected
        else -> error("Unsupported chat invite status: $this")
    }
}
