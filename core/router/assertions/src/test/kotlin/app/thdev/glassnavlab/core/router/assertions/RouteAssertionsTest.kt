package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import org.junit.Test

class RouteAssertionsTest {
    private val feedRoute = TestComposeRoute("feed")
    private val detailRoute = TestComposeRoute("feed/detail")
    private val activityRoute = TestActivityRoute(
        route = "activity/settings",
        activityKey = "settings",
    )

    @Test
    fun routePlanFromStackCreatesComposePlan() {
        val plan = RoutePlan.fromStack(
            stack = RouteStack.of(feedRoute, detailRoute),
        )

        plan.assertRoutePlan {
            hasComposeStack(feedRoute, detailRoute)
            hasNoActivityRoutes()
        }
    }

    @Test
    fun routeStackExposesComposeBackStackOnly() {
        val stack = RouteStack.of(feedRoute, detailRoute)

        stack.assertRouteStack {
            hasEntries(feedRoute, detailRoute)
            hasComposeRoutes(feedRoute, detailRoute)
            containsOnlyComposeRoutes()
        }
    }

    @Test
    fun activityRouteCommandCreatesActivityPlan() {
        val command = RouteCommand(activityRoute)

        command.plan.assertRoutePlan {
            hasNoComposeStack()
            hasActivityRoutes(activityRoute)
        }
    }

    @Test
    fun recordingRouterCapturesCommandsAndResolvedPlans() {
        val router = RecordingRouter()

        router.navigate(RouteCommand(RouteStack.of(feedRoute, detailRoute)))

        router.assertCommandCount(1)
        router.assertLastStack {
            hasEntries(feedRoute, detailRoute)
            containsOnlyComposeRoutes()
        }
        router.assertLastPlan {
            hasComposeStack(feedRoute, detailRoute)
            hasNoActivityRoutes()
        }
    }

    @Test
    fun recordingRouteEventSinkCapturesCallbackEvents() {
        val sink = RecordingRouteEventSink()
        val event = TestRouteEvent("open-detail")

        sink.onRouteEvent(event)

        sink.assertLastEvent(event)
        sink.assertEvents(event)
    }
}
