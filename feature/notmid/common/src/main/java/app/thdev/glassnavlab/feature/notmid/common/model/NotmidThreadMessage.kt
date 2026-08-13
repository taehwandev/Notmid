package app.thdev.glassnavlab.feature.notmid.common.model

data class NotmidThreadMessage(
    val id: String,
    val threadId: String,
    val senderHandle: String,
    val body: String,
    val createdAtLabel: String,
    val mine: Boolean,
    val attachment: NotmidThreadMessageAttachment? = null,
)
