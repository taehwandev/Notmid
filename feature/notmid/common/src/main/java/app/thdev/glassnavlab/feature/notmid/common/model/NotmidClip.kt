package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color

data class NotmidClip(
    val id: String,
    val title: String,
    val description: String,
    val badge: NotmidBadge,
    val palette: List<Color>,
    val isLive: Boolean = false,
    val placeId: String? = null,
    val creatorHandle: String = "",
    val moodTags: List<String> = emptyList(),
    val capturedAtLabel: String = "",
    val qualityLabel: String = "HD",
    val playbackProgress: Float = 0f,
)
