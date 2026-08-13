package app.thdev.glassnavlab.core.auth.impl

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

interface FirebaseIdTokenProvider {
    suspend fun idTokenFor(provider: NotmidAuthProvider): FirebaseIdTokenResult

    fun clearSession() = Unit
}
