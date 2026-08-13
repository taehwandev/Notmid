package app.thdev.glassnavlab.feature.feed

import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace
import app.thdev.glassnavlab.feature.notmid.common.model.labelText

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

private fun compactCount(value: Int): String {
    return if (value >= 1000) {
        "${value / 1000}.${value % 1000 / 100}k"
    } else {
        value.toString()
    }
}
