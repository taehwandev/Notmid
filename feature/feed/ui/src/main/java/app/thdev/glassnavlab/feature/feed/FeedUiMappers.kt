package app.thdev.glassnavlab.feature.feed

import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

internal fun NotmidDestination.toFeedUiState(): FeedUiState {
    val feedPlaces = places.map(NotmidPlace::toFeedPlaceUi)
    val feedClips = clips.map(NotmidClip::toFeedClipUi)

    return FeedUiState(
        title = title,
        subtitle = subtitle,
        heroClip = feedClips.firstOrNull(),
        queue = feedClips.drop(1),
        places = feedPlaces,
    )
}

internal fun FeedUiState.placeFor(clip: FeedClipUi): FeedPlaceUi? {
    return clip.placeId?.let { placeId -> places.firstOrNull { place -> place.id == placeId } }
}

private fun NotmidClip.toFeedClipUi(): FeedClipUi {
    return FeedClipUi(
        id = id,
        title = title,
        caption = description,
        creatorHandle = creatorHandle,
        capturedAtLabel = capturedAtLabel.ifBlank { "Time unavailable" },
        palette = palette,
        moodTags = moodTags,
        placeId = placeId,
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
