package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import app.thdev.glassnavlab.core.designsystem.component.NotmidBottomNavigation
import app.thdev.glassnavlab.core.designsystem.component.NotmidBottomNavigationItem
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassBackdropHost
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import app.thdev.glassnavlab.feature.capture.CaptureScreen
import app.thdev.glassnavlab.feature.capture.api.CaptureRoute
import app.thdev.glassnavlab.feature.feed.ClipDetailScreen
import app.thdev.glassnavlab.feature.feed.FeedScreen
import app.thdev.glassnavlab.feature.feed.api.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.FeedRoute
import app.thdev.glassnavlab.feature.inbox.ChatThreadScreen
import app.thdev.glassnavlab.feature.inbox.InboxScreen
import app.thdev.glassnavlab.feature.inbox.api.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.InboxRoute
import app.thdev.glassnavlab.feature.map.MapScreen
import app.thdev.glassnavlab.feature.map.PlaceDetailScreen
import app.thdev.glassnavlab.feature.map.api.MapRoute
import app.thdev.glassnavlab.feature.map.api.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.NotmidRouteEvent
import app.thdev.glassnavlab.feature.notmid.api.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBackgroundColor
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidGlassIcon
import app.thdev.glassnavlab.feature.notmid.common.model.backdropPaletteForItem
import app.thdev.glassnavlab.feature.notmid.common.model.destinationFor
import app.thdev.glassnavlab.feature.notmid.common.model.notmidPalette
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import app.thdev.glassnavlab.feature.profile.ProfileScreen
import app.thdev.glassnavlab.feature.profile.ProfileSettingsScreen
import app.thdev.glassnavlab.feature.profile.api.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.ProfileSettingsRoute
import app.thdev.glassnavlab.core.router.RouteEvent

@Composable
fun NotmidShellScreen(
    destinations: List<NotmidDestinationModel>,
    authState: NotmidAuthState,
    navigationStack: List<NotmidRoute> = listOf(FeedRoute),
    onRouteEvent: (RouteEvent) -> Unit = {},
    onContinueLocalAuth: () -> Unit = {},
    onBrowseSignedOut: () -> Unit = {},
) {
    val notmidDestinations = remember(destinations) {
        destinations.toNotmidDestinations()
    }
    if (notmidDestinations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NotmidBackgroundColor),
        )
        return
    }

    val items = rememberNotmidNavigationItems(notmidDestinations)
    val selectedDestinationId = navigationStack.lastOrNull()?.selectedDestinationId
        ?: NotmidDestinationIds.FEED
    val activeRoute = navigationStack.lastOrNull() ?: FeedRoute
    val selectedDestination = destinationFor(
        destinations = notmidDestinations,
        selectedItemId = selectedDestinationId,
    )
    val listState = rememberDestinationListState(
        destinationId = selectedDestination.id,
        activeRoute = activeRoute,
    )
    val navigationBackdropColor by rememberNavigationBackdropColor(
        listState = listState,
        destination = selectedDestination,
    )
    val shouldShowLogin = activeRoute.requiresAuth && !authState.isAuthenticated

    LiquidGlassBackdropHost(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidBackgroundColor),
        backgroundColor = NotmidBackgroundColor,
        content = {
            if (shouldShowLogin) {
                NotmidLoginScreen(
                    onContinueLocal = onContinueLocalAuth,
                    onBrowseSignedOut = onBrowseSignedOut,
                )
            } else when (activeRoute) {
                ProfileSettingsRoute -> {
                    ProfileSettingsScreen(
                        parentDestination = selectedDestination,
                        navigationStack = navigationStack,
                        listState = listState,
                    )
                }

                FeedRoute -> {
                    FeedScreen(
                        destination = selectedDestination,
                        listState = listState,
                        onRouteEvent = onRouteEvent,
                    )
                }

                is ClipDetailRoute -> {
                    ClipDetailScreen(
                        destination = selectedDestination,
                        route = activeRoute,
                        listState = listState,
                    )
                }

                MapRoute -> {
                    MapScreen(
                        destination = selectedDestination,
                        listState = listState,
                        onRouteEvent = onRouteEvent,
                    )
                }

                is PlaceDetailRoute -> {
                    PlaceDetailScreen(
                        destination = selectedDestination,
                        route = activeRoute,
                        listState = listState,
                    )
                }

                CaptureRoute -> {
                    CaptureScreen(
                        destination = selectedDestination,
                        listState = listState,
                    )
                }

                InboxRoute -> {
                    InboxScreen(
                        destination = selectedDestination,
                        listState = listState,
                        onRouteEvent = onRouteEvent,
                    )
                }

                is ChatThreadRoute -> {
                    ChatThreadScreen(
                        destination = selectedDestination,
                        route = activeRoute,
                        listState = listState,
                    )
                }

                ProfileRoute -> {
                    ProfileScreen(
                        destination = selectedDestination,
                        listState = listState,
                    )
                }

                else -> {
                    FeedScreen(
                        destination = selectedDestination,
                        listState = listState,
                    )
                }
            }
        },
        floatingContent = { backdrop ->
            if (!shouldShowLogin) {
                NotmidBottomNavigation(
                    items = items,
                    selectedItemId = selectedDestinationId,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    adaptiveBackgroundColor = navigationBackdropColor,
                    onItemSelected = { item ->
                        onRouteEvent(NotmidRouteEvent.DestinationSelected(item.id))
                    },
                )
            }
        },
    )
}

@Composable
private fun rememberDestinationListState(
    destinationId: String,
    activeRoute: NotmidRoute,
): LazyListState {
    val feedListState = rememberLazyListState()
    val mapListState = rememberLazyListState()
    val captureListState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val profileListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    if (activeRoute == ProfileSettingsRoute) {
        return settingsListState
    }
    return when (destinationId) {
        NotmidDestinationIds.MAP -> mapListState
        NotmidDestinationIds.CAPTURE -> captureListState
        NotmidDestinationIds.INBOX -> inboxListState
        NotmidDestinationIds.PROFILE -> profileListState
        else -> feedListState
    }
}

private val NotmidRoute.requiresAuth: Boolean
    get() = when (this) {
        CaptureRoute,
        InboxRoute,
        ProfileRoute,
        ProfileSettingsRoute,
        is ChatThreadRoute,
        -> true

        else -> false
    }

@Composable
private fun rememberNotmidNavigationItems(
    destinations: List<NotmidDestination>,
): List<NotmidBottomNavigationItem> {
    return remember(destinations) {
        destinations.map { destination ->
            NotmidBottomNavigationItem(
                id = destination.id,
                label = destination.title,
                icon = { _, color -> NotmidGlassIcon(destination.icon, color) },
            )
        }
    }
}

@Composable
private fun rememberNavigationBackdropColor(
    listState: LazyListState,
    destination: NotmidDestination,
): State<Color> {
    val density = LocalDensity.current
    val sampleOffsetFromBottom = with(density) {
        NotmidTheme.spacing.bottomNavigationSampleOffset.roundToPx()
    }

    return remember(listState, destination.id, sampleOffsetFromBottom) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val sampleY = layoutInfo.viewportEndOffset - sampleOffsetFromBottom
            val visibleCard = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.index > 0 && sampleY >= item.offset && sampleY <= item.offset + item.size
            } ?: return@derivedStateOf NotmidBackgroundColor
            val palette = backdropPaletteForItem(
                destination = destination,
                itemIndex = visibleCard.index,
            ) ?: return@derivedStateOf NotmidBackgroundColor
            val localFraction = ((sampleY - visibleCard.offset).toFloat() / visibleCard.size)
                .coerceIn(0f, 1f)

            notmidPalette(
                palette = palette,
                fraction = localFraction,
            )
        }
    }
}
