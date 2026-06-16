package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteException
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureModerationStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteDecision
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteResponseReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidChatInviteStatus
import app.thdev.glassnavlab.core.model.notmid.NotmidChatRelationship
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidClipSaveReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidColors
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.core.model.notmid.NotmidProfilePrivacySettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettings
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidSendThreadMessageRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadReceipt
import app.thdev.glassnavlab.core.model.notmid.NotmidStartThreadRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage

class StaticNotmidProtectedWriteRepository(
    private val contentRepository: NotmidContentRepository = StaticNotmidContentRepository(),
) : NotmidProtectedWriteRepository {
    override suspend fun publishCapture(
        authState: NotmidAuthState,
        request: NotmidCapturePublishRequest,
    ): NotmidCapturePublishReceipt {
        return withSession(authState, NotmidProtectedWriteAction.CapturePublish) { session ->
            validateCapturePublishRequest(request)?.let { failure ->
                throw NotmidProtectedWriteException(failure)
            }

            val place = contentRepository.destinations()
                .flatMap { destination -> destination.places }
                .firstOrNull { place -> place.id == request.placeId }
            val clip = NotmidClip(
                id = "receipt-${request.draftId.trim()}",
                title = request.caption.trim().take(48),
                description = request.caption.trim(),
                badge = request.moodTags.first().trim(),
                palette = place?.palette ?: listOf(NotmidColors.DarkCardContent, NotmidColors.White),
                isLive = true,
                placeId = request.placeId.trim(),
                creatorHandle = session.user.handle,
                moodTags = request.moodTags.map(String::trim).filter(String::isNotEmpty),
                capturedAtLabel = "now",
                qualityLabel = "new",
                playbackProgress = 0f,
            )

            NotmidCapturePublishReceipt(
                clip = clip,
                moderationStatus = NotmidCaptureModerationStatus.Queued,
            )
        }
    }

    override suspend fun saveClip(
        authState: NotmidAuthState,
        clipId: String,
    ): NotmidClipSaveReceipt {
        return withSession(authState, NotmidProtectedWriteAction.ClipSave) {
            if (clipId.isBlank()) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ClipSave,
                        code = "missing_clip",
                        message = "clipId is required.",
                    ),
                )
            }

            val clip = contentRepository.destinations()
                .flatMap { destination -> destination.clips }
                .firstOrNull { clip -> clip.id == clipId }
                ?: NotmidClip(
                    id = clipId.trim(),
                    title = "Saved clip",
                    description = "This clip was saved from a protected local action.",
                    badge = "saved",
                    palette = listOf(NotmidColors.DarkCardContent, NotmidColors.White),
                    capturedAtLabel = "now",
                )

            NotmidClipSaveReceipt(clip = clip, saved = true)
        }
    }

    override suspend fun sendThreadMessage(
        authState: NotmidAuthState,
        threadId: String,
        request: NotmidSendThreadMessageRequest,
    ): NotmidSendThreadMessageReceipt {
        return withSession(authState, NotmidProtectedWriteAction.ChatMessage) { session ->
            val body = request.body.trim()
            if (threadId.isBlank()) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatMessage,
                        code = "missing_thread",
                        message = "threadId is required.",
                    ),
                )
            }
            if (body.isBlank()) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatMessage,
                        code = "empty_message",
                        message = "Message body must not be empty.",
                    ),
                )
            }
            val thread = contentRepository.destinations()
                .flatMap { destination -> destination.threads }
                .firstOrNull { thread -> thread.id == threadId.trim() }
            if (thread != null && !thread.chatAccess.canSendMessage) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatMessage,
                        code = "chat_invite_required",
                        message = thread.chatAccess.reasonLabel,
                    ),
                )
            }

            NotmidSendThreadMessageReceipt(
                message = NotmidThreadMessage(
                    id = "msg-${threadId.trim()}-${stableMessageSuffix(body)}",
                    threadId = threadId.trim(),
                    senderHandle = session.user.handle,
                    body = body,
                    createdAtLabel = "now",
                    mine = true,
                    attachment = request.attachment,
                ),
            )
        }
    }

    override suspend fun startThread(
        authState: NotmidAuthState,
        request: NotmidStartThreadRequest,
    ): NotmidStartThreadReceipt {
        return withSession(authState, NotmidProtectedWriteAction.ChatStart) { session ->
            validateStartThreadRequest(request)?.let { failure ->
                throw NotmidProtectedWriteException(failure)
            }

            val participant = staticChatParticipant(request.participantHandle)
                ?: throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatStart,
                        code = "chat_participant_not_found",
                        message = "Chat participant not found.",
                    ),
                )
            if (participant.handle == session.user.handle) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatStart,
                        code = "chat_self_not_allowed",
                        message = "You cannot start a chat with yourself.",
                    ),
                )
            }

            val destinations = contentRepository.destinations()
            validateAttachedContent(request, destinations)?.let { failure ->
                throw NotmidProtectedWriteException(failure)
            }

            val body = request.body.trim()
            val threadId = startThreadId(session.user.handle, participant.handle, request)
            val chatAccess = if (participant.relationship == NotmidChatRelationship.Friend) {
                NotmidChatAccess.AcceptedFriend
            } else {
                NotmidChatAccess(
                    relationship = NotmidChatRelationship.NonFriend,
                    inviteStatus = NotmidChatInviteStatus.PendingOutbound,
                    canSendMessage = false,
                    canAcceptInvite = false,
                    canRejectInvite = false,
                    reasonLabel = "Waiting for this creator to accept the chat request.",
                )
            }

            val thread = NotmidThread(
                id = threadId,
                title = if (participant.relationship == NotmidChatRelationship.Friend) {
                    "chat with ${participant.handle}"
                } else {
                    "request to ${participant.handle}"
                },
                preview = body,
                updatedAtLabel = "now",
                participantHandles = listOf(participant.handle, session.user.handle),
                attachedPlaceId = request.attachedPlaceId,
                attachedClipId = request.attachedClipId,
                chatAccess = chatAccess,
            )
            NotmidStartThreadReceipt(
                thread = thread,
                message = NotmidThreadMessage(
                    id = "msg-$threadId-start",
                    threadId = threadId,
                    senderHandle = session.user.handle,
                    body = body,
                    createdAtLabel = "now",
                    mine = true,
                    attachment = request.toMessageAttachment(),
                ),
            )
        }
    }

    override suspend fun respondThreadInvite(
        authState: NotmidAuthState,
        threadId: String,
        decision: NotmidChatInviteDecision,
    ): NotmidChatInviteResponseReceipt {
        return withSession(authState, NotmidProtectedWriteAction.ChatInviteResponse) {
            val thread = contentRepository.destinations()
                .flatMap { destination -> destination.threads }
                .firstOrNull { thread -> thread.id == threadId.trim() }
                ?: throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatInviteResponse,
                        code = "thread_not_found",
                        message = "Thread not found.",
                    ),
                )

            if (
                thread.chatAccess.inviteStatus != NotmidChatInviteStatus.PendingInbound ||
                (!thread.chatAccess.canAcceptInvite && !thread.chatAccess.canRejectInvite)
            ) {
                throw NotmidProtectedWriteException(
                    invalid(
                        action = NotmidProtectedWriteAction.ChatInviteResponse,
                        code = "chat_invite_not_actionable",
                        message = "This chat request cannot be accepted or rejected.",
                    ),
                )
            }

            val nextAccess = when (decision) {
                NotmidChatInviteDecision.Accept -> NotmidChatAccess(
                    relationship = NotmidChatRelationship.NonFriend,
                    inviteStatus = NotmidChatInviteStatus.Accepted,
                    canSendMessage = true,
                    canAcceptInvite = false,
                    canRejectInvite = false,
                    reasonLabel = "Chat request accepted.",
                )

                NotmidChatInviteDecision.Reject -> NotmidChatAccess(
                    relationship = NotmidChatRelationship.NonFriend,
                    inviteStatus = NotmidChatInviteStatus.Rejected,
                    canSendMessage = false,
                    canAcceptInvite = false,
                    canRejectInvite = false,
                    reasonLabel = "This chat request was rejected.",
                )
            }

            NotmidChatInviteResponseReceipt(
                thread = thread.copy(
                    preview = when (decision) {
                        NotmidChatInviteDecision.Accept -> "Chat request accepted. You can message now."
                        NotmidChatInviteDecision.Reject -> "Chat request rejected."
                    },
                    chatAccess = nextAccess,
                ),
            )
        }
    }

    override suspend fun updateProfileSettings(
        authState: NotmidAuthState,
        request: NotmidProfileSettingsUpdateRequest,
    ): NotmidProfileSettingsUpdateReceipt {
        return withSession(authState, NotmidProtectedWriteAction.ProfileSettings) { session ->
            validateProfileSettingsRequest(request)?.let { failure ->
                throw NotmidProtectedWriteException(failure)
            }

            val user = session.user.copy(
                displayName = request.displayName.trim(),
                homeNeighborhood = request.homeNeighborhood.trim(),
            )
            NotmidProfileSettingsUpdateReceipt(
                settings = NotmidProfileSettings(
                    user = user,
                    privacy = NotmidProfilePrivacySettings(
                        savedPlacesVisibility = "private",
                        chatInvites = "shared-clips-and-places",
                        defaultReceiptVisibility = NotmidCaptureVisibility.Public,
                    ),
                ),
                updated = true,
            )
        }
    }
}

private suspend fun <T> withSession(
    authState: NotmidAuthState,
    action: NotmidProtectedWriteAction,
    block: suspend (NotmidAuthSession) -> T,
): T {
    val session = authState.session ?: throw NotmidProtectedWriteException(
        NotmidProtectedWriteFailure.MissingAuth(action),
    )
    return block(session)
}

private fun validateCapturePublishRequest(
    request: NotmidCapturePublishRequest,
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

private fun validateProfileSettingsRequest(
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

private fun validateStartThreadRequest(
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

private fun validateAttachedContent(
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

private fun invalid(
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

private fun stableMessageSuffix(value: String): String {
    return value.fold(17) { acc, char -> (acc * 31 + char.code).and(0x00FFFFFF) }
        .toString(16)
}

private data class StaticChatParticipant(
    val handle: String,
    val relationship: NotmidChatRelationship,
)

private fun staticChatParticipant(handle: String): StaticChatParticipant? {
    val normalizedHandle = handle.trim()
    return listOf(
        StaticChatParticipant("min.zip", NotmidChatRelationship.Friend),
        StaticChatParticipant("yapmap.ji", NotmidChatRelationship.Friend),
        StaticChatParticipant("receipt.han", NotmidChatRelationship.NonFriend),
    ).firstOrNull { participant -> participant.handle == normalizedHandle }
}

private fun startThreadId(
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

private fun NotmidStartThreadRequest.toMessageAttachment(): NotmidMessageAttachment? {
    val clipId = attachedClipId
    val placeId = attachedPlaceId
    return when {
        clipId != null -> NotmidMessageAttachment.Clip(clipId = clipId)
        placeId != null -> NotmidMessageAttachment.Place(placeId = placeId)
        else -> null
    }
}

private fun slugPart(value: String): String {
    val slug = value
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return slug.ifBlank { "item" }
}
