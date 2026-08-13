package app.thdev.glassnavlab.core.domain.notmid

class NotmidProtectedWriteException(
    val failure: NotmidProtectedWriteFailure,
) : IllegalStateException(failure.toExceptionMessage())

private fun NotmidProtectedWriteFailure.toExceptionMessage(): String {
    return when (this) {
        is NotmidProtectedWriteFailure.MissingAuth -> {
            "notmid protected write requires auth for $action."
        }

        is NotmidProtectedWriteFailure.InvalidRequest -> {
            "notmid protected write request is invalid for $action: $message"
        }

        is NotmidProtectedWriteFailure.HttpStatus -> {
            "notmid protected write failed for $path with HTTP $statusCode."
        }

        is NotmidProtectedWriteFailure.Network -> {
            "notmid protected write network request failed for $path: $message"
        }

        is NotmidProtectedWriteFailure.MalformedResponse -> {
            "notmid protected write response did not match the Android contract for $path."
        }
    }
}
