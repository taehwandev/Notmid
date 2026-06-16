package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest

internal suspend fun <T> withSession(
    authState: NotmidAuthState,
    action: NotmidProtectedWriteAction,
    block: suspend (NotmidAuthSession) -> T,
): T {
    val session = authState.session ?: throw NotmidProtectedWriteException(
        NotmidProtectedWriteFailure.MissingAuth(action),
    )
    return block(session)
}

internal fun validateCapturePublishRequest(
    request: app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest,
): NotmidProtectedWriteFailure.InvalidRequest? {
    return when {
        request.draftId.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.CapturePublish,
            code = "missing_draft",
            message = "draftId is required.",
        )

        request.caption.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.CapturePublish,
            code = "missing_caption",
            message = "caption is required.",
        )

        request.placeId.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.CapturePublish,
            code = "missing_place",
            message = "placeId is required.",
        )

        request.moodTags.none { tag -> tag.isNotBlank() } -> invalid(
            action = NotmidProtectedWriteAction.CapturePublish,
            code = "missing_tags",
            message = "At least one mood tag is required.",
        )

        else -> null
    }
}

internal fun validateProfileSettingsRequest(
    request: NotmidProfileSettingsUpdateRequest,
): NotmidProtectedWriteFailure.InvalidRequest? {
    val displayName = request.displayName.trim()
    val homeNeighborhood = request.homeNeighborhood.trim()
    return when {
        displayName.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.ProfileSettings,
            code = "missing_display_name",
            message = "displayName is required.",
        )

        displayName.length > 80 -> invalid(
            action = NotmidProtectedWriteAction.ProfileSettings,
            code = "display_name_too_long",
            message = "displayName must be 80 characters or fewer.",
        )

        homeNeighborhood.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.ProfileSettings,
            code = "missing_neighborhood",
            message = "homeNeighborhood is required.",
        )

        homeNeighborhood.length > 80 -> invalid(
            action = NotmidProtectedWriteAction.ProfileSettings,
            code = "neighborhood_too_long",
            message = "homeNeighborhood must be 80 characters or fewer.",
        )

        else -> null
    }
}

internal fun validateStartThreadRequest(
    request: NotmidStartThreadRequest,
): NotmidProtectedWriteFailure.InvalidRequest? {
    return when {
        request.participantHandle.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "missing_participant",
            message = "participantHandle is required.",
        )

        request.body.isBlank() -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "empty_message",
            message = "Message body must not be empty.",
        )

        request.attachedClipId?.trim() == "" -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "invalid_clip",
            message = "attachedClipId must not be empty.",
        )

        request.attachedPlaceId?.trim() == "" -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "invalid_place",
            message = "attachedPlaceId must not be empty.",
        )

        else -> null
    }
}

internal fun validateAttachedContent(
    request: NotmidStartThreadRequest,
    destinations: List<NotmidDestination>,
): NotmidProtectedWriteFailure.InvalidRequest? {
    val clipId = request.attachedClipId?.trim()
    val placeId = request.attachedPlaceId?.trim()
    return when {
        clipId != null && destinations.none { destination ->
            destination.clips.any { clip -> clip.id == clipId }
        } -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "clip_not_found",
            message = "Clip not found.",
        )

        placeId != null && destinations.none { destination ->
            destination.places.any { place -> place.id == placeId }
        } -> invalid(
            action = NotmidProtectedWriteAction.ChatStart,
            code = "place_not_found",
            message = "Place not found.",
        )

        else -> null
    }
}

internal fun invalid(
    action: NotmidProtectedWriteAction,
    code: String,
    message: String,
): NotmidProtectedWriteFailure.InvalidRequest {
    return NotmidProtectedWriteFailure.InvalidRequest(
        action = action,
        code = code,
        message = message,
    )
}

internal fun stableMessageSuffix(value: String): String {
    return value.fold(17) { acc, char -> (acc * 31 + char.code).and(0x00FFFFFF) }
        .toString(16)
}

internal data class StaticChatParticipant(
    val handle: String,
    val relationship: NotmidChatRelationship,
)

internal fun staticChatParticipant(handle: String): StaticChatParticipant? {
    val normalizedHandle = handle.trim()
    return listOf(
        StaticChatParticipant("min.zip", NotmidChatRelationship.Friend),
        StaticChatParticipant("yapmap.ji", NotmidChatRelationship.Friend),
        StaticChatParticipant("receipt.han", NotmidChatRelationship.NonFriend),
    ).firstOrNull { participant -> participant.handle == normalizedHandle }
}

internal fun startThreadId(
    actorHandle: String,
    participantHandle: String,
    request: NotmidStartThreadRequest,
): String {
    val handles = listOf(actorHandle, participantHandle).map(::slugPart).sorted()
    val context = request.attachedClipId?.trim()
        ?: request.attachedPlaceId?.trim()
        ?: "direct"
    return "thread-${handles.joinToString("-")}-${slugPart(context)}"
}

internal fun NotmidStartThreadRequest.toMessageAttachment(): NotmidMessageAttachment? {
    val clipId = attachedClipId
    val placeId = attachedPlaceId
    return when {
        clipId != null -> NotmidMessageAttachment.Clip(clipId = clipId)
        placeId != null -> NotmidMessageAttachment.Place(placeId = placeId)
        else -> null
    }
}

internal fun slugPart(value: String): String {
    val slug = value
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return slug.ifBlank { "item" }
}
