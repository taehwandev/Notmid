package app.thdev.glassnavlab.core.model.notmid

data class NotmidSendThreadMessageRequest(
    val body: String,
    val attachment: NotmidMessageAttachment? = null,
)
