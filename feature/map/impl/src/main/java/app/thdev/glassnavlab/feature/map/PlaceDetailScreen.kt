package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidRouteDetailContent
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
fun PlaceDetailScreen(
    destination: NotmidDestination,
    route: PlaceDetailRoute,
    listState: LazyListState,
) {
    val primaryPlace = destination.places.firstOrNull { it.id == route.placeId } ?: NotmidPlace(
        id = route.placeId,
        title = "Place",
        description = "This place route is valid, but the loaded content has no matching item.",
        metric = "missing",
        palette = listOf(NotmidColorTokens.Ink, NotmidColorTokens.Subtle, NotmidColorTokens.Mist),
        height = 176.dp,
        contentColor = NotmidColorTokens.Cloud,
    )
    val primaryClip = destination.clips.firstOrNull { it.placeId == route.placeId }
        ?: destination.clips.firstOrNull()
        ?: NotmidClip(
            id = "place-${route.placeId}-clip",
            title = "Recent proof",
            description = "The loaded content has no proof clip attached to this place.",
            badge = NotmidBadge.Label("place"),
            palette = primaryPlace.palette,
        )

    NotmidRouteDetailContent(
        routeTitle = primaryPlace.title,
        routeSubtitle = primaryPlace.description,
        routeMeta = "places/${route.placeId}",
        primaryClip = primaryClip,
        primaryPlace = primaryPlace,
        listState = listState,
    )
}
