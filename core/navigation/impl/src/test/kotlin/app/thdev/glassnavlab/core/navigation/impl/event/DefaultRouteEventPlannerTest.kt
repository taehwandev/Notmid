package app.thdev.glassnavlab.core.navigation.impl.event

import app.thdev.glassnavlab.core.navigation.assertions.TestComposeRoute
import app.thdev.glassnavlab.core.navigation.assertions.TestRouteEvent
import app.thdev.glassnavlab.core.navigation.assertions.assertRoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan
import app.thdev.glassnavlab.core.navigation.runtime.RouteStack
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultRouteEventPlannerTest {
    @Test
    fun usesFirstHandlerThatReturnsPlan() {
        val route = TestComposeRoute("detail")
        val event = TestRouteEvent("open-detail")
        val planner = DefaultRouteEventPlanner(
            handlers = listOf(
                RouteEventHandler { null },
                RouteEventHandler { receivedEvent ->
                    if (receivedEvent == event) {
                        RoutePlan.compose(RouteStack.single(route))
                    } else {
                        null
                    }
                },
            ),
        )

        val plan = planner.planFor(event)

        plan?.assertRoutePlan {
            hasComposeStack(route)
            hasNoActivityRoutes()
        }
    }

    @Test
    fun returnsNullWhenNoHandlerMatches() {
        val planner = DefaultRouteEventPlanner(
            handlers = listOf(
                RouteEventHandler { null },
            ),
        )

        val plan = planner.planFor(TestRouteEvent("unknown"))

        assertNull(plan)
    }

    @Test
    fun returnsNullWhenNoHandlersAreRegistered() {
        val planner = DefaultRouteEventPlanner(handlers = emptyList())

        val plan = planner.planFor(TestRouteEvent("unknown"))

        assertNull(plan)
    }
}
