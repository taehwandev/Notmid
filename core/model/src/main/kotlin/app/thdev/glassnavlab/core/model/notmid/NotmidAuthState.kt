package app.thdev.glassnavlab.core.model.notmid

data class NotmidAuthState(
    val mode: NotmidAuthMode,
    val session: NotmidAuthSession?,
    val requiredActions: List<NotmidAuthRequiredAction>,
) {
    val isAuthenticated: Boolean
        get() = session != null
}
