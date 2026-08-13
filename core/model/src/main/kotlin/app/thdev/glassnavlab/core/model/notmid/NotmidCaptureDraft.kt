package app.thdev.glassnavlab.core.model.notmid

data class NotmidCaptureDraft(
    val id: String,
    val caption: String,
    val placeId: String?,
    val moodTags: List<String>,
    val visibility: NotmidCaptureVisibility,
    val mediaState: NotmidCaptureMediaState,
    val statusLabel: String,
    val waitTimeLabel: String,
    val crowdLabel: String,
    val priceTierLabel: String,
)
