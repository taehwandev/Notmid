package app.thdev.glassnavlab.core.auth.impl

class UnavailableGoogleIdTokenProvider(
    private val message: String = "Google sign-in is not configured for this Android build.",
) : GoogleIdTokenProvider {
    override suspend fun idToken(): GoogleIdTokenResult {
        return GoogleIdTokenResult.Rejected(
            code = "google_id_token_provider_unavailable",
            message = message,
        )
    }
}
