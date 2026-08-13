package app.thdev.glassnavlab.core.model.notmid

data class NotmidClip(
    val title: String,
    val description: String,
    val badge: String,
    val palette: List<NotmidColor>,
    val isLive: Boolean = false,
    val id: String = title.toStableRouteId(),
    val placeId: String? = null,
    val creatorHandle: String = "",
    val moodTags: List<String> = emptyList(),
    val capturedAtLabel: String = "",
    val qualityLabel: String = "HD",
    val playbackProgress: Float = 0f,
)
