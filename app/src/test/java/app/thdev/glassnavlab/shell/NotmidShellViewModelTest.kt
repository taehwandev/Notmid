package app.thdev.glassnavlab.shell

import app.thdev.glassnavlab.FakeAuthGateway
import app.thdev.glassnavlab.MainDispatcherRule
import app.thdev.glassnavlab.signedOutAuthState
import app.thdev.glassnavlab.core.auth.notmid.NotmidAuthSignInRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthProvider
import app.thdev.glassnavlab.core.runtime.router.planner.AppRoutePlanner
import app.thdev.glassnavlab.core.runtime.router.runtime.DefaultAppRouterRuntime
import app.thdev.glassnavlab.core.navigation.runtime.RouteCommand
import app.thdev.glassnavlab.core.navigation.runtime.RouteEvent
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.feature.feed.api.route.FeedRoute
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.route.ChatThreadRoute
import app.thdev.glassnavlab.feature.inbox.api.route.InboxRoute
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotmidShellViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test fun userActionsProduceRoutesThroughInjectedPort() = runTest(dispatcherRule.dispatcher) {
        val events = mutableListOf<RouteEvent>()
        val vm = NotmidShellViewModel(
            RouteEventSink { events.add(it) },
            testRouter(),
            FakeAuthGateway(signedOutAuthState),
        )
        vm.onAction(NotmidShellAction.DestinationClicked("map"))
        vm.onAction(NotmidShellAction.ThreadClicked("thread-1"))
        vm.onAction(NotmidShellAction.PlaceClicked("place-1"))
        vm.onAction(NotmidShellAction.SettingsClicked)
        assertEquals(listOf(
            NotmidRouteEvent.DestinationSelected("map"),
            InboxRouteEvent.ChatThreadRequested("thread-1"),
            MapRouteEvent.PlaceRequested("place-1"),
            NotmidRouteEvent.SettingsRequested,
        ), events)
    }

    @Test fun authGateAndActiveRouteFollowRouterAndGateway() = runTest(dispatcherRule.dispatcher) {
        val router = testRouter()
        val gateway = FakeAuthGateway(signedOutAuthState)
        val vm = NotmidShellViewModel(RouteEventSink {}, router, gateway)
        assertFalse(vm.state.value.shouldShowLogin)

        router.execute(RoutePlan.compose(RouteStack.of(InboxRoute, ChatThreadRoute("thread-1"))))
        advanceUntilIdle()
        assertEquals(ChatThreadRoute("thread-1"), vm.state.value.activeRoute)
        assertEquals("inbox", vm.state.value.selectedDestinationId)
        assertTrue(vm.state.value.shouldShowLogin)

        gateway.signIn(NotmidAuthSignInRequest(NotmidAuthProvider.Fake))
        advanceUntilIdle()
        assertFalse(vm.state.value.shouldShowLogin)
    }

    private fun testRouter() = DefaultAppRouterRuntime(
        initialStack = RouteStack.single(FeedRoute),
        routePlanner = object : AppRoutePlanner {
            override fun planFor(command: RouteCommand): RoutePlan = RoutePlan.compose(RouteStack.single(FeedRoute))
            override fun planFor(event: RouteEvent): RoutePlan? = null
            override fun planForDeepLink(uriString: String): RoutePlan? = null
        },
    )
}
