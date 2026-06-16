package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

interface FirebaseIdTokenProvider {
    suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult

    fun clearSession() = Unit
}

sealed interface FirebaseIdTokenResult {
    data class Success(
        val token: String,
    ) : FirebaseIdTokenResult

    data class Rejected(
        val code: String,
        val message: String,
    ) : FirebaseIdTokenResult
}

class UnavailableFirebaseIdTokenProvider(
    private val message: String = "Firebase sign-in is not configured for this Android build.",
) : FirebaseIdTokenProvider {
    override suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult {
        return FirebaseIdTokenResult.Rejected(
            code = "firebase_provider_unavailable",
            message = message,
        )
    }
}
