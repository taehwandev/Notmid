package app.thdev.glassnavlab.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.feature.notmid.notice.toSuccessNotice
import app.thdev.glassnavlab.feature.notmid.notice.toProtectedActionNotice
import kotlinx.coroutines.CancellationException
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
    private val writes: NotmidProtectedWriteExecutor,
    private val notices: NoticeEffectDelegate,
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
            ProfileSettingsUiState(value, user?.displayName.orEmpty(), user?.homeNeighborhood.orEmpty(), isSaving = current.isSaving)
        })
    }

    fun onAction(action: ProfileSettingsAction) {
        refreshAuth(auth.currentState())
        val current = state.value
        when (action) {
            is ProfileSettingsAction.DisplayNameChanged -> if (current.auth.isAuthenticated && !current.isSaving) update(current.copy(displayName = action.value))
            is ProfileSettingsAction.NeighborhoodChanged -> if (current.auth.isAuthenticated && !current.isSaving) update(current.copy(homeNeighborhood = action.value))
            ProfileSettingsAction.Save -> if (current.canSave) save(current)
        }
    }

    private fun save(current: ProfileSettingsUiState) {
        val session = current.auth.session ?: return
        update(current.copy(isSaving = true, statusMessage = null))
        val request = NotmidProfileSettingsUpdateRequest(current.displayName, current.homeNeighborhood)
        viewModelScope.launch {
            try {
                val result = writes.execute(current.auth, NotmidProtectedWriteRequest.UpdateProfileSettings(request))
                if (result == NotmidProtectedWriteResult.Busy || auth.currentState().session !== session) return@launch
                check(result is NotmidProtectedWriteResult.ProfileUpdated)
                auth.applyProfileUpdate(session, result.user)
                val notice = NotmidProtectedWriteAction.ProfileSettings.toSuccessNotice()
                update(state.value.copy(statusMessage = notice.message))
                notices.emit(notice.effect)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (auth.currentState().session === session) {
                    val notice = failure.toProtectedActionNotice(NotmidProtectedWriteAction.ProfileSettings)
                    update(state.value.copy(statusMessage = notice.message))
                    notices.emit(notice.effect)
                }
            } finally {
                update(state.value.copy(isSaving = false))
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
