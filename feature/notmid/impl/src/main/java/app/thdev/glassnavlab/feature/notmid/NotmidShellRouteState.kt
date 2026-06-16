package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.feature.capture.api.route.CaptureRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.destinationFor
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute

internal data class NotmidShellRouteState(
    val activeRoute: NotmidRoute,
    val selectedDestinationId: String,
    val selectedDestination: NotmidDestination,
    val listState: LazyListState,
    val shouldShowLogin: Boolean,
)

@Composable
internal fun rememberNotmidShellRouteState(
    destinations: List<NotmidDestination>,
    navigationStack: List<NotmidRoute>,
    authState: NotmidAuthState,
): NotmidShellRouteState {
    val selectedDestinationId = navigationStack.lastOrNull()?.selectedDestinationId
        ?: NotmidDestinationIds.FEED
    val activeRoute = navigationStack.lastOrNull() ?: FeedRoute
    val selectedDestination = destinationFor(
        destinations = destinations,
        selectedItemId = selectedDestinationId,
    )
    val listState = rememberDestinationListState(
        destinationId = selectedDestination.id,
        activeRoute = activeRoute,
    )

    return NotmidShellRouteState(
        activeRoute = activeRoute,
        selectedDestinationId = selectedDestinationId,
        selectedDestination = selectedDestination,
        listState = listState,
        shouldShowLogin = activeRoute.requiresAuth && !authState.isAuthenticated,
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
