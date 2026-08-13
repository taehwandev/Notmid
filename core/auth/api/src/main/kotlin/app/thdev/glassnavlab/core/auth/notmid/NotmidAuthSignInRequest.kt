package app.thdev.glassnavlab.core.auth.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider

data class NotmidAuthSignInRequest(
    val provider: NotmidAuthProvider,
    val intent: NotmidAuthIntent = NotmidAuthIntent.Browse,
    val returnToPath: String? = null,
)
