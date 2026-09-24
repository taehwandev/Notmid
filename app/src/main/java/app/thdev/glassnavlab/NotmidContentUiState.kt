package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.data.notmid.ApiNotmidContentException
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

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
