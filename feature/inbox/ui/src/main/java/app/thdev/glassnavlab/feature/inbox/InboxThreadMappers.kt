package app.thdev.glassnavlab.feature.inbox

import app.thdev.glassnavlab.core.model.notmid.NotmidChatAccess
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
    val serviceThreads = threads.mapIndexed { index, thread ->
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
            routePlan = routePlanFor(index, place),
            messages = messagesByThreadId[thread.id]
                .orEmpty()
                .map { message -> message.toChatMessage(clipById, placeById) },
        )
    }

    if (serviceThreads.isNotEmpty()) {
        return serviceThreads
    }

    val clipThreads = clips.mapIndexed { index, clip ->
        val place = places.getOrNull(index % places.size.coerceAtLeast(1))
        InboxThreadUi(
            id = "thread-${clip.id}",
            title = chatTitleFor(index),
            subtitle = "${place?.title ?: "Open route"} - ${clip.badge.labelText().ifBlank { "clip" }}",
            preview = previewFor(index, place),
            participants = participantsFor(index),
            updatedLabel = updatedLabelFor(index),
            unreadCount = if (index % 2 == 0) index + 1 else 0,
            chatAccess = NotmidChatAccess.AcceptedFriend,
            clip = clip,
            place = place,
            routePlan = routePlanFor(index, place),
        )
    }

    val placeOnlyThreads = if (clipThreads.isEmpty()) {
        places.mapIndexed { index, place ->
            InboxThreadUi(
                id = "thread-${place.id}",
                title = chatTitleFor(index),
                subtitle = "${place.title} - place plan",
                preview = previewFor(index, place),
                participants = participantsFor(index),
                updatedLabel = updatedLabelFor(index),
                unreadCount = if (index == 0) 2 else 0,
                chatAccess = NotmidChatAccess.AcceptedFriend,
                clip = null,
                place = place,
                routePlan = routePlanFor(index, place),
            )
        }
    } else {
        emptyList()
    }

    return (clipThreads + placeOnlyThreads).ifEmpty {
        listOf(fallbackThread("local"))
    }
}

internal fun NotmidDestination.fallbackThread(threadId: String): InboxThreadUi {
    val clip = clips.firstOrNull()
    val place = places.firstOrNull()
    return InboxThreadUi(
        id = "thread-$threadId",
        title = "Local plan",
        subtitle = "${place?.title ?: "Unknown place"} - fake thread",
        preview = "This chat route is valid, but local fake content has no exact thread.",
        participants = "you + crew",
        updatedLabel = "now",
        unreadCount = 0,
        chatAccess = NotmidChatAccess.AcceptedFriend,
        clip = clip,
        place = place,
        routePlan = routePlanFor(0, place),
    )
}

internal fun InboxThreadUi.toMessages(): List<ChatMessageUi> {
    if (messages.isNotEmpty()) {
        return messages
    }

    return listOf(
        ChatMessageUi(
            id = "${id}-fallback-1",
            sender = "Mina",
            body = "This receipt looks current. Does the place still have seats?",
            timestamp = "12:08",
            attachment = clip?.let(ChatAttachmentUi::Clip),
        ),
        ChatMessageUi(
            id = "${id}-fallback-2",
            sender = "You",
            body = "Yes. Window side opened up and the line was under ten minutes.",
            timestamp = "12:11",
            mine = true,
            attachment = place?.let(ChatAttachmentUi::Place),
        ),
        ChatMessageUi(
            id = "${id}-fallback-3",
            sender = "Jae",
            body = "Let's pin it after lunch. I added a short route from the station.",
            timestamp = "12:14",
            attachment = ChatAttachmentUi.RoutePlan(
                title = routePlan,
                description = place?.description ?: "Meet nearby, then decide from the latest clip.",
            ),
        ),
    )
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

private fun chatTitleFor(index: Int): String {
    return listOf(
        "Lunch receipts",
        "After-work route",
        "Coffee check",
        "Gallery hop",
        "Late table plan",
    )[index % 5]
}

private fun participantsFor(index: Int): String {
    return listOf(
        "you + Mina + Jae",
        "you + 4",
        "Nari + you",
        "you + route crew",
    )[index % 4]
}

private fun updatedLabelFor(index: Int): String {
    return listOf("2m", "18m", "1h", "3h", "yesterday")[index % 5]
}

private fun previewFor(index: Int, place: NotmidPlace?): String {
    return listOf(
        "Line moved fast. Worth pulling up if the clip is still live.",
        "Meet at ${place?.title ?: "the pinned spot"} first, then walk the route.",
        "Need one more receipt before we call it.",
        "Place looks calmer than the feed made it seem.",
    )[index % 4]
}

private fun routePlanFor(index: Int, place: NotmidPlace?): String {
    return listOf(
        "Station to ${place?.title ?: "pin"}",
        "Two-stop cafe loop",
        "Quiet-seat backup",
        "Receipt-first route",
    )[index % 4]
}
