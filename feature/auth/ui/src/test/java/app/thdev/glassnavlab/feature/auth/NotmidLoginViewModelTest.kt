package app.thdev.glassnavlab.feature.auth

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import app.thdev.glassnavlab.core.navigation.notmid.NotmidDestinationIds
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidLoginViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun firebasePrimaryAuthRunsOnceAndCompletesFromGateway() = runTest(dispatcherRule.dispatcher) {
        val completion = CompletableDeferred<NotmidAuthResult>()
        val gateway = FakeGateway(NotmidAuthMode.Firebase) { completion.await() }
        val viewModel = NotmidLoginViewModel(gateway, RouteEventSink {}, dispatcherRule.dispatcher)

        viewModel.onAction(NotmidLoginAction.ContinuePrimaryAuth)
        viewModel.onAction(NotmidLoginAction.ContinueGoogleAuth)
        assertTrue(viewModel.state.value.isAuthenticating)
        runCurrent()
        assertEquals(listOf(NotmidAuthProvider.Anonymous), gateway.requests.map { it.provider })

        completion.complete(NotmidAuthResult.Success(gateway.currentState(), "/notmid"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isAuthenticating)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun rejectionIsShownAndBrowseRequestsFeedFromViewModel() = runTest(dispatcherRule.dispatcher) {
        val gateway = FakeGateway(NotmidAuthMode.Fake) {
            NotmidAuthResult.Rejected("auth_disabled", "Authentication is disabled.", currentState())
        }
        val routes = mutableListOf<RouteEvent>()
        val viewModel = NotmidLoginViewModel(gateway, RouteEventSink(routes::add), dispatcherRule.dispatcher)

        viewModel.onAction(NotmidLoginAction.ContinuePrimaryAuth)
        advanceUntilIdle()
        assertEquals(NotmidAuthProvider.Fake, gateway.requests.single().provider)
        assertEquals("Authentication is disabled.", viewModel.state.value.errorMessage)

        viewModel.onAction(NotmidLoginAction.BrowseSignedOut)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf(NotmidRouteEvent.DestinationSelected(NotmidDestinationIds.FEED)), routes)
    }

    @Test
    fun formActionsStayInViewModelAndFailureShowsError() = runTest(dispatcherRule.dispatcher) {
        val gateway = FakeGateway(NotmidAuthMode.Firebase) { throw IllegalStateException("unavailable") }
        val viewModel = NotmidLoginViewModel(gateway, RouteEventSink {}, dispatcherRule.dispatcher)

        viewModel.onAction(NotmidLoginAction.IdentityChanged("handle"))
        viewModel.onAction(NotmidLoginAction.PasswordChanged("placeholder"))
        viewModel.onAction(NotmidLoginAction.PasswordVisibilityToggled)
        assertEquals("handle", viewModel.state.value.identity)
        assertEquals("placeholder", viewModel.state.value.password)
        assertTrue(viewModel.state.value.showPassword)

        viewModel.onAction(NotmidLoginAction.ContinueGoogleAuth)
        advanceUntilIdle()
        assertEquals(NotmidAuthProvider.Google, gateway.requests.single().provider)
        assertEquals("Sign-in failed. Try again.", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isAuthenticating)
        assertEquals("", viewModel.state.value.password)
    }
}

private class FakeGateway(
    mode: NotmidAuthMode,
    private val signInResponse: suspend FakeGateway.() -> NotmidAuthResult,
) : NotmidAuthGateway {
    override val states = MutableStateFlow(NotmidAuthState(mode, null, emptyList()))
    val requests = mutableListOf<NotmidAuthSignInRequest>()

    override fun currentState(): NotmidAuthState = states.value
    override fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState = states.value
    override suspend fun signIn(request: NotmidAuthSignInRequest): NotmidAuthResult {
        requests += request
        return signInResponse()
    }
    override fun signOut(): NotmidAuthState = states.value
}
