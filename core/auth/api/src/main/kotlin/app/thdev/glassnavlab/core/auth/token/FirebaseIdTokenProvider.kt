package app.thdev.glassnavlab.core.auth.token

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

interface FirebaseIdTokenProvider {
    suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult

    fun clearSession() = Unit
}
