package app.thdev.glassnavlab.feature.feed

import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

internal data class FeedUiState(
    val title: String,
    val subtitle: String,
    val heroClip: FeedClipUi?,
    val queue: List<FeedClipUi>,
    val places: List<FeedPlaceUi>,
) {
    val isEmpty: Boolean
        get() = heroClip == null && queue.isEmpty()
}

internal data class FeedClipUi(
    val id: String,
    val title: String,
    val caption: String,
    val creatorHandle: String,
    val badgeLabel: String,
    val capturedAtLabel: String,
    val qualityLabel: String,
    val progress: Float,
    val palette: List<Color>,
    val moodTags: List<String>,
    val placeId: String?,
    val likeCountLabel: String,
    val saveCountLabel: String,
    val chatCountLabel: String,
)

internal data class FeedPlaceUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val metric: String,
    val palette: List<Color>,
)

internal fun NotmidDestination.toFeedUiState(): FeedUiState {
    val feedPlaces = places.map(NotmidPlace::toFeedPlaceUi)
    val feedClips = clips.map { clip ->
        clip.toFeedClipUi(
            fallbackPlace = feedPlaces.firstOrNull(),
        )
    }

    return FeedUiState(
        title = title,
        subtitle = subtitle,
        heroClip = feedClips.firstOrNull(),
        queue = feedClips.drop(1),
        places = feedPlaces,
    )
}

internal fun FeedUiState.placeFor(clip: FeedClipUi): FeedPlaceUi? {
    return clip.placeId
        ?.let { placeId -> places.firstOrNull { place -> place.id == placeId } }
        ?: places.firstOrNull()
}

private fun NotmidClip.toFeedClipUi(
    fallbackPlace: FeedPlaceUi?,
): FeedClipUi {
    val stableSeed = id.fold(0) { acc, char -> acc + char.code }
    return FeedClipUi(
        id = id,
        title = title,
        caption = description,
        creatorHandle = creatorHandle.ifBlank { "receipt.local" },
        badgeLabel = badge.labelText().ifBlank { fallbackPlace?.metric ?: "receipt" },
        capturedAtLabel = capturedAtLabel.ifBlank { if (isLive) "live rn" else "fresh" },
        qualityLabel = qualityLabel,
        progress = playbackProgress.coerceIn(0f, 1f),
        palette = palette,
        moodTags = moodTags.ifEmpty { listOf(badge.labelText()).filter(String::isNotBlank) },
        placeId = placeId ?: fallbackPlace?.id,
        likeCountLabel = compactCount(120 + stableSeed % 880),
        saveCountLabel = compactCount(24 + stableSeed % 240),
        chatCountLabel = compactCount(8 + stableSeed % 90),
    )
}

private fun NotmidPlace.toFeedPlaceUi(): FeedPlaceUi {
    return FeedPlaceUi(
        id = id,
        title = title,
        subtitle = description,
        metric = metric,
        palette = palette,
    )
}

private fun NotmidBadge.labelText(): String {
    return when (this) {
        is NotmidBadge.Label -> text
        NotmidBadge.LiveNow -> "LIVE"
        NotmidBadge.None -> ""
    }
}

private fun compactCount(value: Int): String {
    return if (value >= 1000) {
        "${value / 1000}.${value % 1000 / 100}k"
    } else {
        value.toString()
    }
}
