package app.thdev.glassnavlab.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthIntent
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.feature.auth.di.AuthIoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class NotmidLoginViewModel @Inject constructor(
    private val authGateway: NotmidAuthGateway,
    private val routeEvents: RouteEventSink,
    @param:AuthIoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotmidLoginUiState())
    val state = mutableState.asStateFlow()

    fun onAction(action: NotmidLoginAction) {
        when (action) {
            is NotmidLoginAction.IdentityChanged -> mutableState.update { it.copy(identity = action.value) }
            is NotmidLoginAction.PasswordChanged -> mutableState.update { it.copy(password = action.value) }
            NotmidLoginAction.PasswordVisibilityToggled -> mutableState.update { it.copy(showPassword = !it.showPassword) }
            NotmidLoginAction.ContinuePrimaryAuth -> signIn(primaryAuthProvider())
            NotmidLoginAction.ContinueGoogleAuth -> signIn(NotmidAuthProvider.Google)
            NotmidLoginAction.BrowseSignedOut -> {
                mutableState.update { it.copy(password = "", errorMessage = null) }
                routeEvents.onRouteEvent(NotmidRouteEvent.DestinationSelected(NotmidDestinationIds.FEED))
            }
        }
    }

    private fun primaryAuthProvider(): NotmidAuthProvider = when (authGateway.currentState().mode) {
        NotmidAuthMode.Firebase -> NotmidAuthProvider.Anonymous
        NotmidAuthMode.Fake, NotmidAuthMode.Disabled -> NotmidAuthProvider.Fake
    }

    private fun signIn(provider: NotmidAuthProvider) {
        if (mutableState.value.isAuthenticating) return
        mutableState.update { it.copy(isAuthenticating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    authGateway.signIn(NotmidAuthSignInRequest(provider, NotmidAuthIntent.Browse))
                }
                when (result) {
                    is NotmidAuthResult.Success -> mutableState.update { it.copy(errorMessage = null) }
                    is NotmidAuthResult.Rejected -> mutableState.update { it.copy(errorMessage = result.message) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(errorMessage = "Sign-in failed. Try again.") }
            } finally {
                mutableState.update { it.copy(password = "", isAuthenticating = false) }
            }
        }
    }
}
