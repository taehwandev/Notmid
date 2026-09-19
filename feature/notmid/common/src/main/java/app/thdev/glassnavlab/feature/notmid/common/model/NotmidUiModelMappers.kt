package app.thdev.glassnavlab.feature.notmid.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.model.notmid.NotmidClip as NotmidClipModel
import app.thdev.glassnavlab.core.model.notmid.NotmidColor
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace as NotmidPlaceModel

fun List<NotmidDestinationModel>.toNotmidDestinations(): List<NotmidDestination> {
    return map { it.toUi() }
}

private fun NotmidDestinationModel.toUi(): NotmidDestination {
    return NotmidDestination(
        id = id,
        title = title,
        subtitle = subtitle,
        icon = icon,
        clips = clips.map(NotmidClipModel::toUi),
        places = places.map(NotmidPlaceModel::toUi),
        threads = threads,
        captureDraft = captureDraft,
        threadMessages = threadMessages,
    )
}

private fun NotmidClipModel.toUi(): NotmidClip {
    val uiBadge = when {
        isLive -> NotmidBadge.LiveNow
        badge.trim().isEmpty() -> NotmidBadge.None
        else -> NotmidBadge.Label(badge)
    }
    return NotmidClip(
        id = id,
        title = title,
        description = description,
        badge = uiBadge,
        palette = palette.map(NotmidColor::toColor),
        isLive = isLive,
        placeId = placeId,
        creatorHandle = creatorHandle,
        moodTags = moodTags,
        capturedAtLabel = capturedAtLabel,
        qualityLabel = qualityLabel,
        playbackProgress = playbackProgress.coerceIn(0f, 1f),
    )
}

private fun NotmidPlaceModel.toUi(): NotmidPlace {
    return NotmidPlace(
        id = id,
        title = title,
        description = description,
        metric = metric,
        palette = palette.map(NotmidColor::toColor),
        height = heightDp.dp,
        contentColor = contentColor.toColor(),
        category = category,
        address = address,
        coordinate = coordinate,
        openNow = openNow,
        receiptCount = receiptCount,
    )
}

private fun NotmidColor.toColor(): Color = Color(argb)
