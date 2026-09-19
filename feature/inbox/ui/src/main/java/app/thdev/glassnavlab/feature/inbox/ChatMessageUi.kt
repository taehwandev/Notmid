package app.thdev.glassnavlab.feature.inbox

internal data class ChatMessageUi(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: String,
    val mine: Boolean = false,
    val attachment: ChatAttachmentUi? = null,
)
