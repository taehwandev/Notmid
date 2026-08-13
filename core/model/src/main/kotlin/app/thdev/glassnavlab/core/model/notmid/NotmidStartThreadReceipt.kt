package app.thdev.glassnavlab.core.model.notmid

data class NotmidStartThreadReceipt(
    val thread: NotmidThread,
    val message: NotmidThreadMessage? = null,
)
