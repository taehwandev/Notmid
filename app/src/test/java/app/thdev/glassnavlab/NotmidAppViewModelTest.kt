package app.thdev.glassnavlab

import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthGateway
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthResult
import app.thdev.glassnavlab.core.data.notmid.NotmidContentSource
import app.thdev.glassnavlab.core.data.notmid.ObservableNotmidContentRepository
import app.thdev.glassnavlab.core.domain.notmid.GetNotmidDestinationsUseCase
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffect
import app.thdev.glassnavlab.core.notice.api.effect.NoticeEffectDelegate
import app.thdev.glassnavlab.core.notice.api.effect.MutableNoticeEffectDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.model.notmid.ChannelNotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun gatewayChangesReachAppWithoutScreenCallbacks() = runTest(mainDispatcherRule.dispatcher) {
        val gateway = FakeAuthGateway(signedInAuthState)
        val vm = newViewModel(authGateway = gateway)
        advanceUntilIdle()
        val session = checkNotNull(gateway.currentState().session)
        gateway.applyProfileUpdate(session, session.user.copy(displayName = "Updated elsewhere"))
        advanceUntilIdle()
        assertEquals("Updated elsewhere", vm.state.value.authState.session?.user?.displayName)
        gateway.signOut()
        advanceUntilIdle()
        assertNull(vm.state.value.authState.session)
    }

    @Test
    fun routeAndBrowseActionsAreHandledByViewModel() = runTest(mainDispatcherRule.dispatcher) {
        val events = mutableListOf<RouteEvent>()
        val vm = newViewModel(routeEvents = RouteEventSink { events.add(it) })
        vm.onAction(NotmidAppAction.RouteRequested(NotmidRouteEvent.SettingsRequested))
        vm.onAction(NotmidAppAction.BrowseSignedOut)
        advanceUntilIdle()
        assertEquals(listOf(NotmidRouteEvent.SettingsRequested, NotmidRouteEvent.DestinationSelected("feed")), events)
    }

    @Test
    fun initLoadsContentIntoState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        )

        advanceUntilIdle()

        assertEquals(
            NotmidContentUiState.Ready(
                source = NotmidContentSource.Static,
                destinations = listOf(viewModelTestDestination),
            ),
            viewModel.state.value.content,
        )
    }

    @Test
    fun contentCancellationIsNotMappedToErrorState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = newViewModel(
            contentRepository = object : NotmidContentRepository {
                override suspend fun destinations(): List<NotmidDestination> {
                    throw CancellationException("content load cancelled")
                }
            },
        )

        advanceUntilIdle()

        assertEquals(NotmidContentUiState.Loading, viewModel.state.value.content)
    }

    @Test
    fun rejectedAuthUpdatesAuthErrorState() = runTest(mainDispatcherRule.dispatcher) {
        val rejectedState = signedOutAuthState
        val viewModel = newViewModel(
            authGateway = FakeAuthGateway(
                initialState = signedOutAuthState,
                signInResult = NotmidAuthResult.Rejected(
                    code = "auth_disabled",
                    message = "Authentication is disabled for this runtime.",
                    state = rejectedState,
                ),
            ),
        )

        viewModel.onAction(NotmidAppAction.ContinueAuth(NotmidAuthProvider.Fake))
        advanceUntilIdle()

        assertEquals(
            "Authentication is disabled for this runtime.",
            viewModel.state.value.authErrorMessage,
        )
        assertFalse(viewModel.state.value.isAuthenticating)
    }

    @Test
    fun actionsAreProcessedThroughInjectedActionDelegate() = runTest(mainDispatcherRule.dispatcher) {
        val actions = ChannelNotmidActionDelegate<NotmidAppAction>()
        val events = mutableListOf<RouteEvent>()
        newViewModel(actionDelegate = actions, routeEvents = RouteEventSink { events.add(it) })
        actions.dispatch(NotmidAppAction.RouteRequested(NotmidRouteEvent.SettingsRequested))
        advanceUntilIdle()
        assertEquals(listOf(NotmidRouteEvent.SettingsRequested), events)
    }

    @Test
    fun injectedFeatureNoticeStreamIsExposedWithoutAppWriteHandling() = runTest(mainDispatcherRule.dispatcher) {
        val notices = MutableNoticeEffectDelegate()
        val vm = newViewModel(uiEffects = notices)
        val effects = mutableListOf<NoticeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        val effect = NoticeEffect.NavigateDeepLink("https://thdev.app/notmid/inbox")
        notices.emit(effect)
        advanceUntilIdle()
        assertEquals(listOf(effect), effects)
    }

    private fun newViewModel(
        contentRepository: NotmidContentRepository = FakeContentRepository(listOf(viewModelTestDestination)),
        authGateway: NotmidAuthGateway = FakeAuthGateway(signedInAuthState),
        actionDelegate: NotmidActionDelegate<NotmidAppAction> = ChannelNotmidActionDelegate(),
        uiEffects: NoticeEffectDelegate = MutableNoticeEffectDelegate(),
        routeEvents: RouteEventSink = RouteEventSink {},
    ): NotmidAppViewModel {
        val sharedContent = ObservableNotmidContentRepository(contentRepository)
        return NotmidAppViewModel(
            contentSource = NotmidContentSource.Static,
            getDestinations = GetNotmidDestinationsUseCase(sharedContent),
            contentUpdates = sharedContent,
            authGateway = authGateway,
            actionDelegate = actionDelegate,
            uiEffects = uiEffects,
            ioDispatcher = mainDispatcherRule.dispatcher,
            routeEvents = routeEvents,
        )
    }
}
