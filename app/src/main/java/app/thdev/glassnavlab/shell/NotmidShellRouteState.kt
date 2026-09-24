package app.thdev.glassnavlab.shell

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRoute
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.destinationFor
import app.thdev.glassnavlab.feature.profile.api.route.ProfileSettingsRoute

internal data class NotmidShellRouteState(
    val selectedDestination: NotmidDestination,
    val listState: LazyListState,
)

@Composable
internal fun rememberNotmidShellRouteState(
    destinations: List<NotmidDestination>,
    shellState: NotmidShellUiState,
): NotmidShellRouteState {
    val selectedDestination = destinationFor(
        destinations = destinations,
        selectedItemId = shellState.selectedDestinationId,
    )
    val listState = rememberDestinationListState(
        destinationId = selectedDestination.id,
        activeRoute = shellState.activeRoute,
    )

    return NotmidShellRouteState(
        selectedDestination = selectedDestination,
        listState = listState,
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
