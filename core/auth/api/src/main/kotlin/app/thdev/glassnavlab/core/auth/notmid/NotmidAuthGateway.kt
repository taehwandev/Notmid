package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

interface NotmidAuthGateway {
    fun currentState(): NotmidAuthState

    suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult

    fun signOut(): NotmidAuthState
}
