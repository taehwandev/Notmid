package app.thdev.glassnavlab.core.auth.impl

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

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
