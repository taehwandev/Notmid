package app.thdev.glassnavlab.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassBackdropHost
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBackgroundColor
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import app.thdev.glassnavlab.core.designsystem.component.backdrop.rememberListBackdropColor
import androidx.compose.ui.graphics.Color

@Composable
fun NotmidShellScreen(
    destinations: List<NotmidDestinationModel>,
) {
    val viewModel: NotmidShellViewModel = viewModel()
    val shellState by viewModel.state.collectAsStateWithLifecycle()
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

    val routeState = rememberNotmidShellRouteState(
        destinations = notmidDestinations,
        shellState = shellState,
    )
    val navigationBackdropColor by rememberListBackdropColor(
        listState = routeState.listState,
        palettes = routeState.selectedDestination.let { destination ->
            listOf(emptyList<Color>()) + destination.clips.map { it.palette } + destination.places.map { it.palette }
        },
    )
    var feedBackdropColor by remember { mutableStateOf(NotmidBackgroundColor) }
    var mapBackdropColor by remember { mutableStateOf(NotmidBackgroundColor) }

    LiquidGlassBackdropHost(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidBackgroundColor),
        backgroundColor = NotmidBackgroundColor,
        content = {
            NotmidRouteContent(
                shellState = shellState,
                onFeedBackdropColorChanged = { feedBackdropColor = it },
                onMapBackdropColorChanged = { mapBackdropColor = it },
            )
        },
        floatingContent = { backdrop ->
            if (!shellState.shouldShowLogin) {
                NotmidShellBottomNavigation(
                    destinations = notmidDestinations,
                    selectedDestinationId = shellState.selectedDestinationId,
                    navigationBackdropColor = when (shellState.activeRoute) {
                        FeedRoute, is ClipDetailRoute -> feedBackdropColor
                        MapRoute, is PlaceDetailRoute -> mapBackdropColor
                        else -> navigationBackdropColor
                    },
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onAction = viewModel::onAction,
                )
            }
        },
    )
}
