package app.thdev.glassnavlab.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.snapshotFlow
import app.thdev.glassnavlab.core.activity.route.PendingActivityRouteRequest
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.router.runtime.AppRouterRuntime
import app.thdev.glassnavlab.router.notmidRouteStack
import app.thdev.glassnavlab.feature.capture.api.route.CaptureRoute
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class NotmidShellViewModel @Inject constructor(
    private val routeEvents: RouteEventSink,
    private val router: AppRouterRuntime,
    private val authGateway: NotmidAuthGateway,
) : ViewModel() {
    val state = combine(
        snapshotFlow { router.backStack to router.pendingActivityRouteRequest },
        authGateway.states,
    ) { (stack, activityRouteRequest), authState ->
        shellUiState(stack.entries.filterIsInstance<NotmidRoute>(), authState, activityRouteRequest)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = shellUiState(
            router.notmidRouteStack(),
            authGateway.currentState(),
            router.pendingActivityRouteRequest,
        ),
    )

    fun onAction(action: NotmidShellAction) {
        val event = when (action) {
            is NotmidShellAction.DestinationClicked -> NotmidRouteEvent.DestinationSelected(action.destinationId)
            is NotmidShellAction.ThreadClicked -> InboxRouteEvent.ChatThreadRequested(action.threadId)
            is NotmidShellAction.PlaceClicked -> MapRouteEvent.PlaceRequested(action.placeId)
            is NotmidShellAction.DeepLinkRequested -> {
                action.uri.takeIf(String::isNotBlank)?.let(router::navigateDeepLink)
                return
            }
            is NotmidShellAction.ActivityRouteLaunched -> {
                router.consumeActivityRouteRequest(action.requestId)
                return
            }
            NotmidShellAction.SettingsClicked -> NotmidRouteEvent.SettingsRequested
        }
        routeEvents.onRouteEvent(event)
    }
}

private fun shellUiState(
    navigationStack: List<NotmidRoute>,
    authState: NotmidAuthState,
    activityRouteRequest: PendingActivityRouteRequest?,
): NotmidShellUiState {
    val activeRoute = navigationStack.lastOrNull() ?: FeedRoute
    return NotmidShellUiState(
        activeRoute = activeRoute,
        selectedDestinationId = activeRoute.selectedDestinationId.ifBlank { NotmidDestinationIds.FEED },
        shouldShowLogin = activeRoute.requiresAuth && !authState.isAuthenticated,
        settingsRouteLabel = if (activeRoute == ProfileSettingsRoute) {
            navigationStack.joinToString(" > ") { it.deepLinkPathSegments.last() }
        } else {
            ""
        },
        activityRouteRequest = activityRouteRequest,
    )
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
