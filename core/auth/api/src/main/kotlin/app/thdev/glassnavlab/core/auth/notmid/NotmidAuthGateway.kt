package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

interface NotmidAuthGateway {
    fun currentState(): NotmidAuthState

    suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult

    fun signOut(): NotmidAuthState
}

data class NotmidAuthSignInRequest(
    val provider: NotmidAuthProvider,
    val intent: NotmidAuthIntent = NotmidAuthIntent.Browse,
    val returnToPath: String? = null,
)

enum class NotmidAuthIntent {
    Browse,
    Capture,
    Chat,
    Profile,
}

sealed interface NotmidAuthResult {
    data class Success(
        val state: NotmidAuthState,
        val nextPath: String,
    ) : NotmidAuthResult

    data class Rejected(
        val code: String,
        val message: String,
        val state: NotmidAuthState,
    ) : NotmidAuthResult
}
