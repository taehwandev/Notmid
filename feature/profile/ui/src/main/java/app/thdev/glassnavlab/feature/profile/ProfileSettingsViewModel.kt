package app.thdev.glassnavlab.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidProfileSettingsUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class ProfileSettingsViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val auth: NotmidAuthGateway,
    private val writes: NotmidActionDelegate<NotmidProtectedWriteRequest>,
) : ViewModel() {
    private val initialAuth = auth.currentState()
    private val initialUser = initialAuth.session?.user
    private val restore = initialUser != null && saved.get<String>("userId") == initialUser.id
    private val mutableState = MutableStateFlow(ProfileSettingsUiState(
        auth = initialAuth,
        displayName = if (restore) saved["name"] ?: initialUser?.displayName.orEmpty() else initialUser?.displayName.orEmpty(),
        homeNeighborhood = if (restore) saved["neighborhood"] ?: initialUser?.homeNeighborhood.orEmpty() else initialUser?.homeNeighborhood.orEmpty(),
    ))
    val state = mutableState.asStateFlow()

    init { viewModelScope.launch { auth.states.collect(::refreshAuth) } }

    private fun refreshAuth(value: NotmidAuthState) {
        val user = value.session?.user
        val current = state.value
        update(if (current.auth.session?.user?.id == user?.id) current.copy(auth = value) else {
            ProfileSettingsUiState(value, user?.displayName.orEmpty(), user?.homeNeighborhood.orEmpty())
        })
    }

    fun onAction(action: ProfileSettingsAction) {
        refreshAuth(auth.currentState())
        val current = state.value
        when (action) {
            is ProfileSettingsAction.DisplayNameChanged -> if (current.auth.isAuthenticated) update(current.copy(displayName = action.value))
            is ProfileSettingsAction.NeighborhoodChanged -> if (current.auth.isAuthenticated) update(current.copy(homeNeighborhood = action.value))
            ProfileSettingsAction.Save -> if (current.canSave) {
                val request = NotmidProfileSettingsUpdateRequest(current.displayName, current.homeNeighborhood)
                viewModelScope.launch { writes.dispatch(NotmidProtectedWriteRequest.UpdateProfileSettings(request)) }
            }
        }
    }

    private fun update(value: ProfileSettingsUiState) {
        mutableState.value = value
        saved["userId"] = value.auth.session?.user?.id
        saved["name"] = value.displayName
        saved["neighborhood"] = value.homeNeighborhood
    }
}
