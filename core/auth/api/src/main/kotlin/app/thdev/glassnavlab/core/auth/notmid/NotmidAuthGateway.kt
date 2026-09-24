package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import kotlinx.coroutines.flow.StateFlow

interface NotmidAuthGateway {
    val states: StateFlow<NotmidAuthState>

    fun currentState(): NotmidAuthState

    /** Apply a verified receipt only for the exact session instance read when the request began. */
    fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState

    suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult

    fun signOut(): NotmidAuthState
}
