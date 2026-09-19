package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassBackdropHost
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination as NotmidDestinationModel
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBackgroundColor
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import app.thdev.glassnavlab.core.designsystem.component.backdrop.rememberListBackdropColor
import androidx.compose.ui.graphics.Color

@Composable
fun NotmidShellScreen(
    destinations: List<NotmidDestinationModel>,
    authState: NotmidAuthState,
    authErrorMessage: String? = null,
    isAuthenticating: Boolean = false,
    isPublishingCapture: Boolean = false,
    isSavingClip: Boolean = false,
    isSendingMessage: Boolean = false,
    isStartingChat: Boolean = false,
    isRespondingChatInvite: Boolean = false,
    isSavingProfileSettings: Boolean = false,
    capturePublishMessage: String? = null,
    clipSaveMessage: String? = null,
    chatMessage: String? = null,
    profileSettingsMessage: String? = null,
    navigationStack: List<NotmidRoute> = listOf(FeedRoute),
    onRouteEvent: (RouteEvent) -> Unit = {},
    onContinueLocalAuth: () -> Unit = {},
    onContinueGoogleAuth: () -> Unit = onContinueLocalAuth,
    onBrowseSignedOut: () -> Unit = {},
    onPublishCapture: (
        draftId: String,
        caption: String,
        placeId: String,
        moodTags: List<String>,
        visibility: String,
    ) -> Unit = { _, _, _, _, _ -> },
    onSaveClip: (String) -> Unit = {},
    onAcceptThreadInvite: (String) -> Unit = {},
    onRejectThreadInvite: (String) -> Unit = {},
    onSendThreadMessage: (threadId: String, body: String) -> Unit = { _, _ -> },
    onStartThread: (
        participantHandle: String,
        body: String,
        attachedClipId: String?,
        attachedPlaceId: String?,
    ) -> Unit = { _, _, _, _ -> },
    onUpdateProfileSettings: (displayName: String, homeNeighborhood: String) -> Unit = { _, _ -> },
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

    val routeState = rememberNotmidShellRouteState(
        destinations = notmidDestinations,
        navigationStack = navigationStack,
        authState = authState,
    )
    val navigationBackdropColor by rememberListBackdropColor(
        listState = routeState.listState,
        palettes = routeState.selectedDestination.let { destination ->
            listOf(emptyList<Color>()) + destination.clips.map { it.palette } + destination.places.map { it.palette }
        },
    )
    var feedBackdropColor by remember { mutableStateOf(NotmidBackgroundColor) }

    LiquidGlassBackdropHost(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidBackgroundColor),
        backgroundColor = NotmidBackgroundColor,
        content = {
            NotmidRouteContent(
                routeState = routeState,
                navigationStack = navigationStack,
                authState = authState,
                authErrorMessage = authErrorMessage,
                isAuthenticating = isAuthenticating,
                isPublishingCapture = isPublishingCapture,
                isSavingClip = isSavingClip,
                isSendingMessage = isSendingMessage,
                isStartingChat = isStartingChat,
                isRespondingChatInvite = isRespondingChatInvite,
                isSavingProfileSettings = isSavingProfileSettings,
                capturePublishMessage = capturePublishMessage,
                clipSaveMessage = clipSaveMessage,
                chatMessage = chatMessage,
                profileSettingsMessage = profileSettingsMessage,
                onRouteEvent = onRouteEvent,
                onFeedBackdropColorChanged = { feedBackdropColor = it },
                onContinueLocalAuth = onContinueLocalAuth,
                onContinueGoogleAuth = onContinueGoogleAuth,
                onBrowseSignedOut = onBrowseSignedOut,
                onPublishCapture = onPublishCapture,
                onSaveClip = onSaveClip,
                onAcceptThreadInvite = onAcceptThreadInvite,
                onRejectThreadInvite = onRejectThreadInvite,
                onSendThreadMessage = onSendThreadMessage,
                onStartThread = onStartThread,
                onUpdateProfileSettings = onUpdateProfileSettings,
            )
        },
        floatingContent = { backdrop ->
            if (!routeState.shouldShowLogin) {
                NotmidShellBottomNavigation(
                    destinations = notmidDestinations,
                    selectedDestinationId = routeState.selectedDestinationId,
                    navigationBackdropColor = if (routeState.activeRoute == FeedRoute) {
                        feedBackdropColor
                    } else navigationBackdropColor,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onRouteEvent = { event -> onRouteEvent(event) },
                )
            }
        },
    )
}
