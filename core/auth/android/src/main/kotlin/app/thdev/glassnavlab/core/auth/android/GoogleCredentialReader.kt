package app.thdev.glassnavlab.core.auth.android

internal interface GoogleCredentialReader {
    suspend fun idToken(serverClientId: String): GoogleCredentialReaderResult
}
