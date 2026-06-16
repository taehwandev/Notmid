package app.thdev.glassnavlab.feature.notmid

import androidx.compose.runtime.Composable
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.router.RouteEvent
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
import app.thdev.glassnavlab.feature.map.api.MapRouteEvent
import app.thdev.glassnavlab.feature.map.api.PlaceDetailRoute
import app.thdev.glassnavlab.feature.notmid.api.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.api.NotmidRouteEvent
import app.thdev.glassnavlab.feature.profile.ProfileScreen
import app.thdev.glassnavlab.feature.profile.ProfileSettingsScreen
import app.thdev.glassnavlab.feature.profile.api.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.ProfileSettingsRoute

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
    onRouteEvent: (RouteEvent) -> Unit,
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
    onStartThread: (
        participantHandle: String,
        body: String,
        attachedClipId: String?,
        attachedPlaceId: String?,
    ) -> Unit,
    onUpdateProfileSettings: (displayName: String, homeNeighborhood: String) -> Unit,
) {
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
            ProfileSettingsScreen(
                parentDestination = routeState.selectedDestination,
                authState = authState,
                navigationStack = navigationStack,
                listState = routeState.listState,
                isSaving = isSavingProfileSettings,
                statusMessage = profileSettingsMessage,
                onSaveProfileSettings = onUpdateProfileSettings,
            )
        }

        FeedRoute -> {
            FeedScreen(
                destination = routeState.selectedDestination,
                listState = routeState.listState,
                onRouteEvent = onRouteEvent,
            )
        }

        is ClipDetailRoute -> {
            ClipDetailScreen(
                destination = routeState.selectedDestination,
                route = route,
                listState = routeState.listState,
                isStartingChat = isStartingChat,
                onStartThread = onStartThread,
            )
        }

        MapRoute -> {
            MapScreen(
                destination = routeState.selectedDestination,
                listState = routeState.listState,
                onRouteEvent = onRouteEvent,
            )
        }

        is PlaceDetailRoute -> {
            PlaceDetailScreen(
                destination = routeState.selectedDestination,
                route = route,
                listState = routeState.listState,
            )
        }

        CaptureRoute -> {
            CaptureScreen(
                destination = routeState.selectedDestination,
                listState = routeState.listState,
                isPublishing = isPublishingCapture,
                publishStatusMessage = capturePublishMessage,
                onPublish = onPublishCapture,
            )
        }

        InboxRoute -> {
            InboxScreen(
                destination = routeState.selectedDestination,
                listState = routeState.listState,
                onRouteEvent = onRouteEvent,
            )
        }

        is ChatThreadRoute -> {
            ChatThreadScreen(
                destination = routeState.selectedDestination,
                route = route,
                listState = routeState.listState,
                isSavingClip = isSavingClip,
                isSendingMessage = isSendingMessage,
                isRespondingChatInvite = isRespondingChatInvite,
                clipSaveMessage = clipSaveMessage,
                chatMessage = chatMessage,
                onSaveClip = onSaveClip,
                onOpenPlace = { placeId ->
                    onRouteEvent(MapRouteEvent.PlaceRequested(placeId))
                },
                onAcceptInvite = onAcceptThreadInvite,
                onRejectInvite = onRejectThreadInvite,
                onSendMessage = onSendThreadMessage,
            )
        }

        ProfileRoute -> {
            ProfileScreen(
                destination = routeState.selectedDestination,
                authState = authState,
                listState = routeState.listState,
                onSettingsRequested = {
                    onRouteEvent(NotmidRouteEvent.SettingsRequested)
                },
            )
        }

        else -> {
            FeedScreen(
                destination = routeState.selectedDestination,
                listState = routeState.listState,
            )
        }
    }
}
