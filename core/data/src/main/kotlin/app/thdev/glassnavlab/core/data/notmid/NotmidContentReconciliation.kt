package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage

internal fun List<NotmidDestination>.withWriteResult(
    result: NotmidProtectedWriteResult,
): List<NotmidDestination> = when (result) {
    is NotmidProtectedWriteResult.MessageSent -> withMessage(result.message)
    is NotmidProtectedWriteResult.ThreadStarted -> withThread(result.thread).let { destinations ->
        result.message?.let(destinations::withMessage) ?: destinations
    }
    is NotmidProtectedWriteResult.ThreadUpdated -> withThread(result.thread)
    NotmidProtectedWriteResult.Busy, NotmidProtectedWriteResult.Completed, is NotmidProtectedWriteResult.ProfileUpdated -> this
}

private fun List<NotmidDestination>.withMessage(message: NotmidThreadMessage) = map { destination ->
    val ownsThread = destination.threads.any { it.id == message.threadId } ||
        destination.threadMessages.any { it.threadId == message.threadId }
    if (!ownsThread) destination else destination.copy(
        threads = destination.threads.map { thread ->
            if (thread.id == message.threadId) {
                thread.copy(preview = message.body, updatedAtLabel = message.createdAtLabel)
            } else thread
        },
        threadMessages = destination.threadMessages.filterNot { it.id == message.id } + message,
    )
}

private fun List<NotmidDestination>.withThread(thread: NotmidThread): List<NotmidDestination> {
    val clip = thread.attachedClipId?.let { id ->
        firstNotNullOfOrNull { destination -> destination.clips.firstOrNull { it.id == id } }
    }
    val place = thread.attachedPlaceId?.let { id ->
        firstNotNullOfOrNull { destination -> destination.places.firstOrNull { it.id == id } }
    } ?: clip?.placeId?.let { id ->
        firstNotNullOfOrNull { destination -> destination.places.firstOrNull { it.id == id } }
    }
    return map { destination ->
        val exists = destination.threads.any { it.id == thread.id }
        val receives = exists || destination.icon == NotmidNavigationIcon.Inbox ||
            destination.clips.any { it.id == thread.attachedClipId } ||
            destination.places.any { it.id == thread.attachedPlaceId }
        if (!receives) destination else destination.copy(
            threads = if (exists) {
                destination.threads.map { if (it.id == thread.id) thread else it }
            } else listOf(thread) + destination.threads,
            clips = if (clip != null && destination.clips.none { it.id == clip.id }) {
                destination.clips + clip
            } else destination.clips,
            places = if (place != null && destination.places.none { it.id == place.id }) {
                destination.places + place
            } else destination.places,
        )
    }
}
