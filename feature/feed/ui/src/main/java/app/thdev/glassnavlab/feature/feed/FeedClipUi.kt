package app.thdev.glassnavlab.feature.feed

import androidx.compose.ui.graphics.Color

internal data class FeedClipUi(
    val id: String,
    val title: String,
    val caption: String,
    val creatorHandle: String,
    val capturedAtLabel: String,
    val palette: List<Color>,
    val moodTags: List<String>,
    val placeId: String?,
)
