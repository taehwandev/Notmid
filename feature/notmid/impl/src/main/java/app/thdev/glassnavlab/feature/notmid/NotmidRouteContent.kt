package app.thdev.glassnavlab.feature.notmid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
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
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute
import app.thdev.glassnavlab.feature.profile.ProfileScreen
import app.thdev.glassnavlab.feature.profile.ProfileSettingsScreen
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute

@Composable
internal fun NotmidRouteContent(
    routeState: NotmidShellRouteState,
    navigationStack: List<NotmidRoute>,
    authState: NotmidAuthState,
    authErrorMessage: String?,
    isAuthenticating: Boolean,
    isPublishingCapture: Boolean,
    isSavingClip: Boolean,
    isSendingMessage: Boolean,
    isStartingChat: Boolean,
    isRespondingChatInvite: Boolean,
    isSavingProfileSettings: Boolean,
    capturePublishMessage: String?,
    clipSaveMessage: String?,
    chatMessage: String?,
    profileSettingsMessage: String?,
    onAction: (NotmidShellAction) -> Unit,
    onFeedBackdropColorChanged: (Color) -> Unit,
    onMapBackdropColorChanged: (Color) -> Unit,
    onContinueLocalAuth: () -> Unit,
    onContinueGoogleAuth: () -> Unit,
    onBrowseSignedOut: () -> Unit,
    onPublishCapture: (
        draftId: String,
        caption: String,
        placeId: String,
        moodTags: List<String>,
        visibility: String,
    ) -> Unit,
    onSaveClip: (String) -> Unit,
    onAcceptThreadInvite: (String) -> Unit,
    onRejectThreadInvite: (String) -> Unit,
    onSendThreadMessage: (threadId: String, body: String) -> Unit,
    onUpdateProfileSettings: (displayName: String, homeNeighborhood: String) -> Unit,
) {
    val feedStateHolder = rememberSaveableStateHolder()
    if (routeState.shouldShowLogin) {
        NotmidLoginScreen(
            errorMessage = authErrorMessage,
            isAuthenticating = isAuthenticating,
            onContinueLocal = onContinueLocalAuth,
            onContinueGoogle = onContinueGoogleAuth,
            onBrowseSignedOut = onBrowseSignedOut,
        )
        return
    }

    when (val route = routeState.activeRoute) {
        ProfileSettingsRoute -> {
            feedStateHolder.SaveableStateProvider("profile-settings") {
                ProfileSettingsScreen(
                    navigationStack = navigationStack,
                    isSaving = isSavingProfileSettings,
                    statusMessage = profileSettingsMessage,
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
                    isStartingChat = isStartingChat,
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
                CaptureScreen(
                    isPublishing = isPublishingCapture,
                    publishStatusMessage = capturePublishMessage,
                )
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
                    isSavingClip = isSavingClip,
                    isSendingMessage = isSendingMessage,
                    isRespondingChatInvite = isRespondingChatInvite,
                    clipSaveMessage = clipSaveMessage,
                    chatMessage = chatMessage,
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
