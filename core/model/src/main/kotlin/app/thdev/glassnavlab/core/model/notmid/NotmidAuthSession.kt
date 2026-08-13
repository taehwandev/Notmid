package app.thdev.glassnavlab.core.model.notmid

data class NotmidAuthSession(
    val accessToken: String,
    val provider: NotmidAuthProvider,
    val expiresAt: String,
    val user: NotmidAuthUser,
)
