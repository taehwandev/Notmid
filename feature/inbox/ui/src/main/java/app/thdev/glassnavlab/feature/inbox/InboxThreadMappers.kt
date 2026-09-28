package app.thdev.glassnavlab.feature.inbox

import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import app.thdev.glassnavlab.core.model.notmid.NotmidMessageAttachment
import app.thdev.glassnavlab.feature.notmid.common.model.labelText

internal fun NotmidDestination.toInboxThreads(): List<InboxThreadUi> {
    val clipById = clips.associateBy(NotmidClip::id)
    val placeById = places.associateBy(NotmidPlace::id)
    val messagesByThreadId = threadMessages.groupBy(NotmidThreadMessage::threadId)
    val serviceThreads = threads.map { thread ->
        val clip = clipById[thread.attachedClipId]
        val place = placeById[thread.attachedPlaceId] ?: clip?.placeId?.let(placeById::get)
        InboxThreadUi(
            id = thread.id,
            title = thread.title,
            subtitle = "${place?.title ?: "Open route"} - ${clip?.badge?.labelText().orEmpty().ifBlank { "thread" }}",
            preview = thread.preview,
            participants = thread.participantHandles.joinToString(" + "),
            updatedLabel = thread.updatedAtLabel,
            unreadCount = thread.unreadCount,
            chatAccess = thread.chatAccess,
            clip = clip,
            place = place,
            routePlan = place?.title ?: "Shared place",
            messages = messagesByThreadId[thread.id]
                .orEmpty()
                .map { message -> message.toChatMessage(clipById, placeById) },
        )
    }

    return serviceThreads
}

internal fun InboxThreadUi.toMessages(): List<ChatMessageUi> {
    return messages
}

private fun NotmidThreadMessage.toChatMessage(
    clips: Map<String, NotmidClip>,
    places: Map<String, NotmidPlace>,
): ChatMessageUi {
    return ChatMessageUi(
        id = id,
        sender = if (mine) "You" else senderHandle,
        body = body,
        timestamp = createdAtLabel,
        mine = mine,
        attachment = attachment?.toChatAttachment(clips, places),
    )
}

private fun NotmidMessageAttachment.toChatAttachment(
    clips: Map<String, NotmidClip>,
    places: Map<String, NotmidPlace>,
): ChatAttachmentUi? {
    return when (this) {
        is NotmidMessageAttachment.Clip -> {
            clips[clipId]?.let(ChatAttachmentUi::Clip)
        }

        is NotmidMessageAttachment.Place -> {
            places[placeId]?.let(ChatAttachmentUi::Place)
        }

        is NotmidMessageAttachment.Route -> {
            ChatAttachmentUi.RoutePlan(
                title = title,
                description = placeIds.mapNotNull { placeId ->
                    places[placeId]?.title
                }.joinToString(" -> ").ifBlank {
                    "Route shared from the notmid API."
                },
            )
        }
    }
}
