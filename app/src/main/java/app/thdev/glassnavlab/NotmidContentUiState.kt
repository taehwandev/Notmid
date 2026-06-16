package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentException
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage

internal sealed interface NotmidContentUiState {
    data object Loading : NotmidContentUiState

    data class Ready(
        val source: NotmidContentSource,
        val destinations: List<NotmidDestination>,
    ) : NotmidContentUiState

    data class Error(
        val source: NotmidContentSource,
        val title: String,
        val message: String,
    ) : NotmidContentUiState
}

internal fun notmidContentReadyOrError(
    source: NotmidContentSource,
    destinations: List<NotmidDestination>,
): NotmidContentUiState {
    return if (destinations.isEmpty()) {
        NotmidContentUiState.Error(
            source = source,
            title = "No content",
            message = "${source.label} returned no notmid destinations.",
        )
    } else {
        NotmidContentUiState.Ready(
            source = source,
            destinations = destinations,
        )
    }
}

internal fun notmidContentError(
    source: NotmidContentSource,
    throwable: Throwable,
): NotmidContentUiState.Error {
    return NotmidContentUiState.Error(
        source = source,
        title = "${source.label} unavailable",
        message = throwable.toNotmidContentMessage(),
    )
}

internal fun NotmidContentUiState.withThreadMessage(
    message: NotmidThreadMessage,
): NotmidContentUiState {
    return when (this) {
        is NotmidContentUiState.Ready -> {
            copy(
                destinations = destinations.map { destination ->
                    destination.withThreadMessage(message)
                },
            )
        }

        NotmidContentUiState.Loading,
        is NotmidContentUiState.Error,
        -> this
    }
}

internal fun NotmidContentUiState.withThread(
    thread: NotmidThread,
): NotmidContentUiState {
    return when (this) {
        is NotmidContentUiState.Ready -> {
            val context = destinations.contextFor(thread)
            copy(
                destinations = destinations.map { destination ->
                    destination.withThread(thread, context)
                },
            )
        }

        NotmidContentUiState.Loading,
        is NotmidContentUiState.Error,
        -> this
    }
}

private fun NotmidDestination.withThreadMessage(
    message: NotmidThreadMessage,
): NotmidDestination {
    val ownsThread = threads.any { thread -> thread.id == message.threadId } ||
        threadMessages.any { threadMessage -> threadMessage.threadId == message.threadId }

    if (!ownsThread) {
        return this
    }

    return copy(
        threads = threads.map { thread ->
            if (thread.id == message.threadId) {
                thread.copy(
                    preview = message.body,
                    updatedAtLabel = message.createdAtLabel,
                )
            } else {
                thread
            }
        },
        threadMessages = threadMessages
            .filterNot { threadMessage -> threadMessage.id == message.id } + message,
    )
}

private fun NotmidDestination.withThread(
    thread: NotmidThread,
    context: NotmidThreadContext,
): NotmidDestination {
    if (threads.any { currentThread -> currentThread.id == thread.id }) {
        return copy(
            threads = threads.map { currentThread ->
                if (currentThread.id == thread.id) thread else currentThread
            },
        ).withThreadContext(thread, context)
    }

    if (!shouldReceiveThread(thread)) {
        return this
    }

    return copy(threads = listOf(thread) + threads)
        .withThreadContext(thread, context)
}

private fun NotmidDestination.shouldReceiveThread(thread: NotmidThread): Boolean {
    return icon == NotmidNavigationIcon.Inbox ||
        thread.attachedClipId?.let { clipId ->
            clips.any { clip -> clip.id == clipId }
        } == true ||
        thread.attachedPlaceId?.let { placeId ->
            places.any { place -> place.id == placeId }
        } == true
}

private data class NotmidThreadContext(
    val clip: NotmidClip?,
    val place: NotmidPlace?,
)

private fun List<NotmidDestination>.contextFor(
    thread: NotmidThread,
): NotmidThreadContext {
    val clip = thread.attachedClipId?.let { clipId ->
        firstNotNullOfOrNull { destination ->
            destination.clips.firstOrNull { clip -> clip.id == clipId }
        }
    }
    val place = thread.attachedPlaceId?.let { placeId ->
        firstNotNullOfOrNull { destination ->
            destination.places.firstOrNull { place -> place.id == placeId }
        }
    } ?: clip?.placeId?.let { placeId ->
        firstNotNullOfOrNull { destination ->
            destination.places.firstOrNull { place -> place.id == placeId }
        }
    }
    return NotmidThreadContext(
        clip = clip,
        place = place,
    )
}

private fun NotmidDestination.withThreadContext(
    thread: NotmidThread,
    context: NotmidThreadContext,
): NotmidDestination {
    val contextClip = context.clip
        ?.takeIf { clip -> thread.attachedClipId == clip.id }
        ?.takeUnless { clip -> clips.any { currentClip -> currentClip.id == clip.id } }
    val contextPlace = context.place
        ?.takeIf { place ->
            thread.attachedPlaceId == place.id || context.clip?.placeId == place.id
        }
        ?.takeUnless { place -> places.any { currentPlace -> currentPlace.id == place.id } }

    if (contextClip == null && contextPlace == null) {
        return this
    }

    return copy(
        clips = if (contextClip == null) clips else clips + contextClip,
        places = if (contextPlace == null) places else places + contextPlace,
    )
}

internal val NotmidContentSource.label: String
    get() = when (this) {
        NotmidContentSource.Static -> "Local content"
        NotmidContentSource.Api -> "notmid API"
    }

private fun Throwable.toNotmidContentMessage(): String {
    return when (this) {
        is ApiNotmidContentException.HttpStatus -> {
            "The notmid API returned HTTP $statusCode for $path."
        }

        is ApiNotmidContentException.Network -> {
            "The notmid API request for $path failed: ${error.message}"
        }

        is ApiNotmidContentException.MalformedJson -> {
            "The notmid API response for $path does not match the Android contract."
        }

        else -> message ?: "Content could not be loaded."
    }
}
