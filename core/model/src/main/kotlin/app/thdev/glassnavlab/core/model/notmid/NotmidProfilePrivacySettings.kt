package app.thdev.glassnavlab.core.model.notmid

data class NotmidProfilePrivacySettings(
    val savedPlacesVisibility: String,
    val chatInvites: String,
    val defaultReceiptVisibility: NotmidCaptureVisibility,
)
