package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidColors
import app.thdev.glassnavlab.core.model.notmid.NotmidGeoPoint
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidProfilePrivacySettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettings
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import kotlinx.serialization.json.JsonObject

/**
 * Decoders from notmid API JSON objects to core models.
 *
 * The content and protected-write repositories hydrate the same models from the
 * same wire shapes, so they share one decoder set instead of keeping two copies
 * that can drift apart.
 */
internal fun JsonObject.toClip(): NotmidClip {
    val id = requiredString("id")
    val metrics = requiredObject("metrics")
    val capturedAtLabel = requiredString("capturedAtLabel")
    return NotmidClip(
        id = id,
        title = requiredString("title"),
        description = requiredString("caption"),
        badge = optionalStringArray("moodTags").firstOrNull() ?: capturedAtLabel,
        palette = paletteForStableId(id),
        isLive = capturedAtLabel.contains("m") || capturedAtLabel == "now",
        placeId = requiredString("placeId"),
        creatorHandle = requiredString("creatorHandle"),
        moodTags = optionalStringArray("moodTags"),
        capturedAtLabel = capturedAtLabel,
        qualityLabel = metrics.optionalString("distanceLabel") ?: "HD",
        playbackProgress = stableProgressFor(id),
    )
}

internal fun JsonObject.toPlace(): NotmidPlace {
    val id = requiredString("id")
    val category = requiredString("category")
    val neighborhood = requiredString("neighborhood")
    val openNow = requiredBoolean("openNow")
    val receiptCount = requiredInt("receiptCount")
    return NotmidPlace(
        id = id,
        title = requiredString("name"),
        description = "$category in $neighborhood",
        metric = requiredInt("score").toString(),
        palette = paletteForStableId(id),
        heightDp = 136 + (receiptCount % 4) * 18,
        contentColor = if (openNow) NotmidColors.White else NotmidColors.DarkCardContent,
        category = category,
        address = requiredString("address"),
        coordinate = NotmidGeoPoint(
            latitude = requiredDouble("lat"),
            longitude = requiredDouble("lng"),
        ),
        openNow = openNow,
        receiptCount = receiptCount,
    )
}

internal fun JsonObject.toThread(): NotmidThread {
    return NotmidThread(
        id = requiredString("id"),
        title = requiredString("title"),
        preview = requiredString("preview"),
        updatedAtLabel = requiredString("updatedAtLabel"),
        participantHandles = requiredStringArray("participantHandles"),
        attachedPlaceId = optionalString("attachedPlaceId"),
        attachedClipId = optionalString("attachedClipId"),
        unreadCount = requiredInt("unreadCount"),
        chatAccess = optionalObject("chatAccess")?.toChatAccess()
            ?: NotmidChatAccess.AcceptedFriend,
    )
}

internal fun JsonObject.toChatAccess(): NotmidChatAccess {
    return NotmidChatAccess(
        relationship = requiredString("relationship").toChatRelationship(),
        inviteStatus = requiredString("inviteStatus").toChatInviteStatus(),
        canSendMessage = requiredBoolean("canSendMessage"),
        canAcceptInvite = requiredBoolean("canAcceptInvite"),
        canRejectInvite = requiredBoolean("canRejectInvite"),
        reasonLabel = requiredString("reasonLabel"),
    )
}

internal fun JsonObject.toThreadMessage(): NotmidThreadMessage {
    return NotmidThreadMessage(
        id = requiredString("id"),
        threadId = requiredString("threadId"),
        senderHandle = requiredString("senderHandle"),
        body = requiredString("body"),
        createdAtLabel = requiredString("createdAtLabel"),
        mine = requiredBoolean("mine"),
        attachment = optionalObject("attachment")?.toMessageAttachment(),
    )
}

internal fun JsonObject.toMessageAttachment(): NotmidMessageAttachment {
    val type = requiredString("type")
    return when (type) {
        "clip" -> NotmidMessageAttachment.Clip(
            clipId = requiredString("clipId"),
        )

        "place" -> NotmidMessageAttachment.Place(
            placeId = requiredString("placeId"),
        )

        "route" -> NotmidMessageAttachment.Route(
            title = requiredString("title"),
            placeIds = requiredStringArray("placeIds"),
        )

        else -> error("Unsupported message attachment type: $type")
    }
}

internal fun JsonObject.toCaptureDraft(): NotmidCaptureDraft {
    return NotmidCaptureDraft(
        id = requiredString("id"),
        caption = requiredString("caption"),
        placeId = optionalString("placeId"),
        moodTags = requiredStringArray("moodTags"),
        visibility = requiredString("visibility").toCaptureVisibility(),
        mediaState = requiredString("mediaState").toCaptureMediaState(),
        statusLabel = "Draft synced from API",
        waitTimeLabel = "pending",
        crowdLabel = "live",
        priceTierLabel = "$$",
    )
}

internal fun JsonObject.toProfileSettings(): NotmidProfileSettings {
    return NotmidProfileSettings(
        user = requiredObject("user").toAuthUser(),
        privacy = requiredObject("privacy").toPrivacySettings(),
    )
}

internal fun JsonObject.toAuthUser(): NotmidAuthUser {
    return NotmidAuthUser(
        id = requiredString("id"),
        handle = requiredString("handle"),
        displayName = requiredString("displayName"),
        homeNeighborhood = requiredString("homeNeighborhood"),
        avatarImageUrl = requiredString("avatarImageUrl"),
        roles = requiredStringArray("roles"),
    )
}

internal fun JsonObject.toPrivacySettings(): NotmidProfilePrivacySettings {
    return NotmidProfilePrivacySettings(
        savedPlacesVisibility = requiredString("savedPlacesVisibility"),
        chatInvites = requiredString("chatInvites"),
        defaultReceiptVisibility = requiredString("defaultReceiptVisibility").toCaptureVisibility(),
    )
}
