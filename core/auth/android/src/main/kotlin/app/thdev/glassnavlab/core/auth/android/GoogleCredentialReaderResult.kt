package app.thdev.glassnavlab.core.auth.android

internal sealed interface GoogleCredentialReaderResult {
    data class Success(
        val idToken: String,
    ) : GoogleCredentialReaderResult

    data class Failure(
        val code: String,
        val message: String,
    ) : GoogleCredentialReaderResult

    data object Cancelled : GoogleCredentialReaderResult
}
