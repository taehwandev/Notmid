package app.thdev.glassnavlab.coreapp.router.runtime

import app.thdev.glassnavlab.core.router.assertions.TestActivityRoute
import app.thdev.glassnavlab.core.router.assertions.TestComposeRoute
import app.thdev.glassnavlab.core.router.assertions.FakeRouteEventPlanner
import app.thdev.glassnavlab.core.router.assertions.TestRouteEvent
import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RouteEvent
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import app.thdev.glassnavlab.coreapp.router.deeplink.FakeAppDeepLinkResolver
import app.thdev.glassnavlab.coreapp.router.planner.DefaultAppRoutePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAppRouterRuntimeTest {
    private val homeRoute = TestComposeRoute("home")
    private val detailRoute = TestComposeRoute("detail")

    @Test
    fun startsWithInitialStack() {
        val router = createRuntime()

        assertEquals(
            listOf(homeRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun routeCommandReplacesComposeStack() {
        val router = createRuntime()

        router.navigate(RouteCommand(RouteStack.single(detailRoute)))

        assertEquals(
            listOf(detailRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun routeEventUsesInjectedEventPlanner() {
        val event = TestRouteEvent("open-detail")
        val router = createRuntime(
            eventPlans = mapOf(
                event to RoutePlan.compose(RouteStack.single(detailRoute)),
            ),
        )

        router.onRouteEvent(event)

        assertEquals(
            listOf(detailRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun deepLinkUsesInjectedResolver() {
        val router = createRuntime(
            deepLinkPlans = mapOf(
                "https://example.test/detail" to RoutePlan.compose(RouteStack.single(detailRoute)),
            ),
        )

        router.navigateDeepLink("https://example.test/detail")

        assertEquals(
            listOf(detailRoute),
            router.backStack.entries,
        )
    }

    @Test
    fun activityRouteQueuesPendingRequestWithoutReplacingStack() {
        val route = TestActivityRoute(
            route = "activity",
            activityKey = "test",
        )
        val router = createRuntime()

        router.navigate(RouteCommand(route))

        assertEquals(
            listOf(homeRoute),
            router.backStack.entries,
        )
        assertEquals(route, router.pendingActivityRouteRequest?.route)
    }

    @Test
    fun consumedActivityRouteClearsPendingRequest() {
        val route = TestActivityRoute(
            route = "activity",
            activityKey = "test",
        )
        val router = createRuntime()

        router.navigate(RouteCommand(route))
        val requestId = router.pendingActivityRouteRequest?.id ?: error("Missing activity request.")

        router.consumeActivityRouteRequest(requestId)

        assertNull(router.pendingActivityRouteRequest)
    }

    private fun createRuntime(
        eventPlans: Map<RouteEvent, RoutePlan> = emptyMap(),
        deepLinkPlans: Map<String, RoutePlan> = emptyMap(),
    ): DefaultAppRouterRuntime {
        return DefaultAppRouterRuntime(
            initialStack = RouteStack.single(homeRoute),
            routePlanner = DefaultAppRoutePlanner(
                routeEventPlanner = FakeRouteEventPlanner(eventPlans),
                deepLinkResolver = FakeAppDeepLinkResolver(deepLinkPlans),
            ),
        )
    }
}
