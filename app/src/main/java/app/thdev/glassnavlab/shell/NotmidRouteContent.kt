package app.thdev.glassnavlab.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.feature.auth.NotmidLoginScreen
import app.thdev.glassnavlab.feature.capture.CaptureScreen
import app.thdev.glassnavlab.feature.capture.api.route.CaptureRoute
import app.thdev.glassnavlab.feature.feed.ClipDetailScreen
import app.thdev.glassnavlab.feature.feed.FeedScreen
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.ChatThreadScreen
import app.thdev.glassnavlab.feature.inbox.InboxScreen
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.map.MapScreen
import app.thdev.glassnavlab.feature.map.PlaceDetailScreen
import app.thdev.glassnavlab.feature.map.api.route.MapRoute
import app.thdev.glassnavlab.feature.map.api.route.PlaceDetailRoute
import app.thdev.glassnavlab.feature.profile.ProfileScreen
import app.thdev.glassnavlab.feature.profile.ProfileSettingsScreen
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute

@Composable
internal fun NotmidRouteContent(
    shellState: NotmidShellUiState,
    onFeedBackdropColorChanged: (Color) -> Unit,
    onMapBackdropColorChanged: (Color) -> Unit,
) {
    val feedStateHolder = rememberSaveableStateHolder()
    if (shellState.shouldShowLogin) {
        NotmidLoginScreen()
        return
    }

    when (val route = shellState.activeRoute) {
        ProfileSettingsRoute -> {
            feedStateHolder.SaveableStateProvider("profile-settings") {
                ProfileSettingsScreen(
                    navigationStack = shellState.navigationStack,
                )
            }
        }

        FeedRoute -> {
            feedStateHolder.SaveableStateProvider("feed") {
                FeedScreen(
                    onBackdropColorChanged = onFeedBackdropColorChanged,
                )
            }
        }

        is ClipDetailRoute -> {
            feedStateHolder.SaveableStateProvider(route.route) {
                ClipDetailScreen(
                    route = route,
                    onBackdropColorChanged = onFeedBackdropColorChanged,
                )
            }
        }

        MapRoute -> {
            feedStateHolder.SaveableStateProvider("map") {
                MapScreen(onBackdropColorChanged = onMapBackdropColorChanged)
            }
        }

        is PlaceDetailRoute -> {
            feedStateHolder.SaveableStateProvider(route.route) {
                PlaceDetailScreen(route = route, onBackdropColorChanged = onMapBackdropColorChanged)
            }
        }

        CaptureRoute -> {
            feedStateHolder.SaveableStateProvider("capture") {
                CaptureScreen()
            }
        }

        InboxRoute -> {
            feedStateHolder.SaveableStateProvider("inbox") {
                InboxScreen()
            }
        }

        is ChatThreadRoute -> {
            feedStateHolder.SaveableStateProvider(route.route) {
                ChatThreadScreen(
                    route = route,
                )
            }
        }

        ProfileRoute -> {
            feedStateHolder.SaveableStateProvider("profile") { ProfileScreen() }
        }

        else -> {
            feedStateHolder.SaveableStateProvider("feed") {
                FeedScreen(onBackdropColorChanged = onFeedBackdropColorChanged)
            }
        }
    }
}
