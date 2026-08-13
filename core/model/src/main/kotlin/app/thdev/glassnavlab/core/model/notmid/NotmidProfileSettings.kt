package app.thdev.glassnavlab.core.model.notmid

data class NotmidProfileSettings(
    val user: NotmidAuthUser,
    val privacy: NotmidProfilePrivacySettings,
)
