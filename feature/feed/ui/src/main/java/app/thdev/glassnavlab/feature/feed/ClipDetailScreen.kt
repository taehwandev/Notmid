package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidGlassIcon
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidRouteDetailContent
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
fun ClipDetailScreen(
    destination: NotmidDestination,
    route: ClipDetailRoute,
    listState: LazyListState,
    isStartingChat: Boolean = false,
    onStartThread: (
        participantHandle: String,
        body: String,
        attachedClipId: String?,
        attachedPlaceId: String?,
    ) -> Unit = { _, _, _, _ -> },
) {
    val primaryClip = destination.clips.firstOrNull { it.id == route.clipId } ?: NotmidClip(
        id = route.clipId,
        title = "Clip",
        description = "This clip route is valid, but the loaded content has no matching item.",
        badge = NotmidBadge.Label("missing"),
        palette = listOf(NotmidColorTokens.Ink, NotmidColorTokens.Subtle, NotmidColorTokens.Mist),
    )
    val primaryPlace = primaryClip.placeId
        ?.let { placeId -> destination.places.firstOrNull { it.id == placeId } }
        ?: destination.places.firstOrNull()
        ?: NotmidPlace(
            id = "clip-${route.clipId}-place",
            title = "Linked place",
            description = "The loaded content has no place attached to this clip.",
            metric = "clip",
            palette = primaryClip.palette,
            height = 176.dp,
            contentColor = NotmidColorTokens.Cloud,
        )

    NotmidRouteDetailContent(
        routeTitle = primaryClip.title,
        routeSubtitle = primaryClip.description,
        routeMeta = "clips/${route.clipId}",
        primaryClip = primaryClip,
        primaryPlace = primaryPlace,
        listState = listState,
        actions = {
            NotmidButton(
                text = if (isStartingChat) "Starting" else "Chat",
                onClick = {
                    onStartThread(
                        primaryClip.creatorHandle,
                        "Can we chat about ${primaryClip.title}?",
                        primaryClip.id,
                        primaryClip.placeId,
                    )
                },
                enabled = primaryClip.creatorHandle.isNotBlank() && !isStartingChat,
                variant = NotmidButtonVariant.Secondary,
                leadingIcon = { color ->
                    NotmidGlassIcon(NotmidNavigationIcon.Inbox, color)
                },
            )
        },
    )
}
