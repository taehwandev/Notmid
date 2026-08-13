package app.thdev.glassnavlab.auth

internal interface GoogleCredentialReader {
    suspend fun idToken(serverClientId: String): GoogleCredentialReaderResult
}
