package app.thdev.glassnavlab.core.model.notmid

data class NotmidThreadMessage(
    val id: String,
    val threadId: String,
    val senderHandle: String,
    val body: String,
    val createdAtLabel: String,
    val mine: Boolean,
    val attachment: NotmidMessageAttachment? = null,
)
