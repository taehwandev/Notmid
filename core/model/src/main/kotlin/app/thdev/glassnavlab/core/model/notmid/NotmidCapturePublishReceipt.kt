package app.thdev.glassnavlab.core.model.notmid

data class NotmidCapturePublishReceipt(
    val clip: NotmidClip,
    val moderationStatus: NotmidCaptureModerationStatus,
)
