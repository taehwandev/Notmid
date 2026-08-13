package app.thdev.glassnavlab.feature.feed

import androidx.compose.ui.graphics.Color

internal data class FeedPlaceUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val metric: String,
    val palette: List<Color>,
)
