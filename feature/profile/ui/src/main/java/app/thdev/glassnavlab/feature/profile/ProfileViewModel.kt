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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    auth: NotmidAuthGateway,
    content: NotmidContentUpdates,
    private val routes: RouteEventSink,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(ProfileTab.Clips)

    val state = combine(auth.states, content.snapshot, selectedTab) { session, snapshot, tab ->
        val destinations = (snapshot as? NotmidContentSnapshot.Ready)?.destinations?.toNotmidDestinations().orEmpty()
        val destination = destinations.firstOrNull { it.id == ProfileRoute.selectedDestinationId }
        val handle = session.session?.user?.handle
        val ownClips = destination?.clips.orEmpty().filter { clip ->
            !handle.isNullOrBlank() && clip.creatorHandle.equals(handle, ignoreCase = true)
        }
        ProfileUiState(session, destination, tab, ownClips)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(auth.currentState()))

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SelectTab -> selectedTab.value = action.tab
            ProfileAction.OpenSettings -> routes.onRouteEvent(NotmidRouteEvent.SettingsRequested)
        }
    }
}
