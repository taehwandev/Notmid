package app.thdev.glassnavlab.core.model.notmid

data class NotmidCapturePublishRequest(
    val draftId: String,
    val caption: String,
    val placeId: String,
    val moodTags: List<String>,
    val visibility: NotmidCaptureVisibility,
)
