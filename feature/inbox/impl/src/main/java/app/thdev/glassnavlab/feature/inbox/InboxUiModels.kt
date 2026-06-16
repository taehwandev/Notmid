package app.thdev.glassnavlab.feature.inbox

import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidChatAccess
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidThreadMessage
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidThreadMessageAttachment

internal data class InboxThreadUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val participants: String,
    val updatedLabel: String,
    val unreadCount: Int,
    val chatAccess: NotmidChatAccess,
    val clip: NotmidClip?,
    val place: NotmidPlace?,
    val routePlan: String,
    val messages: List<ChatMessageUi> = emptyList(),
)

internal data class ChatMessageUi(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: String,
    val mine: Boolean = false,
    val attachment: ChatAttachmentUi? = null,
)

internal sealed interface ChatAttachmentUi {
    data class Clip(val clip: NotmidClip) : ChatAttachmentUi
    data class Place(val place: NotmidPlace) : ChatAttachmentUi
    data class RoutePlan(val title: String, val description: String) : ChatAttachmentUi
}

internal fun List<InboxThreadUi>.filterFor(filter: String): List<InboxThreadUi> {
    return when (filter) {
        "Unread" -> filter { it.unreadCount > 0 }
        "Clips" -> filter { it.clip != null }
        "Places" -> filter { it.place != null }
        else -> this
    }
}

internal fun List<InboxThreadUi>.findMatchingThread(threadId: String): InboxThreadUi? {
    return firstOrNull { thread ->
        thread.id == threadId ||
            thread.clip?.id == threadId ||
            thread.place?.id == threadId
    }
}

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

private fun NotmidThreadMessageAttachment.toChatAttachment(
    clips: Map<String, NotmidClip>,
    places: Map<String, NotmidPlace>,
): ChatAttachmentUi? {
    return when (this) {
        is NotmidThreadMessageAttachment.Clip -> {
            clips[clipId]?.let(ChatAttachmentUi::Clip)
        }

        is NotmidThreadMessageAttachment.Place -> {
            places[placeId]?.let(ChatAttachmentUi::Place)
        }

        is NotmidThreadMessageAttachment.Route -> {
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

internal fun InboxThreadUi.palette(): List<Color> {
    return clip?.palette?.takeIf { it.isNotEmpty() }
        ?: place?.palette?.takeIf { it.isNotEmpty() }
        ?: listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
}

internal fun NotmidBadge.labelText(): String {
    return when (this) {
        is NotmidBadge.Label -> text
        NotmidBadge.LiveNow -> "LIVE"
        NotmidBadge.None -> ""
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
