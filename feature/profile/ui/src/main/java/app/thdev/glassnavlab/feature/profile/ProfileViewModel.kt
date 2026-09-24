package app.thdev.glassnavlab.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.profile.api.route.ProfileRoute
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    auth: NotmidAuthGateway,
    content: NotmidContentUpdates,
    private val routes: RouteEventSink,
) : ViewModel() {
    val state = combine(auth.states, content.snapshot) { session, snapshot ->
        val destinations = (snapshot as? NotmidContentSnapshot.Ready)?.destinations?.toNotmidDestinations().orEmpty()
        ProfileUiState(session, destinations.firstOrNull { it.id == ProfileRoute.selectedDestinationId } ?: destinations.firstOrNull())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(auth.currentState()))

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OpenSettings -> routes.onRouteEvent(NotmidRouteEvent.SettingsRequested)
        }
    }
}
