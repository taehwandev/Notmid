package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ActivityRoute
import app.thdev.glassnavlab.core.router.route.ComposeRoute
import app.thdev.glassnavlab.core.router.runtime.RoutePlan

class RoutePlanSubject(
    private val plan: RoutePlan,
) {
    fun hasComposeStack(vararg expected: ComposeRoute) {
        val stack = plan.composeStack ?: fail("Expected a compose route stack.")
        RouteStackSubject(stack).hasEntries(*expected)
    }

    fun hasNoComposeStack() {
        if (plan.composeStack != null) {
            fail("Expected no compose route stack, but found ${plan.composeStack}.")
        }
    }

    fun hasActivityRoutes(vararg expected: ActivityRoute) {
        assertEquals(
            expected = expected.toList(),
            actual = plan.activityRoutes,
            label = "activity routes",
        )
    }

    fun hasNoActivityRoutes() {
        if (plan.activityRoutes.isNotEmpty()) {
            fail("Expected no activity routes, but found ${plan.activityRoutes}.")
        }
    }

    fun hasLaunchOptions(
        launchSingleTop: Boolean,
        restoreState: Boolean,
    ) {
        assertEquals(
            expected = launchSingleTop,
            actual = plan.launchSingleTop,
            label = "launchSingleTop",
        )
        assertEquals(
            expected = restoreState,
            actual = plan.restoreState,
            label = "restoreState",
        )
    }
}

fun RoutePlan.assertRoutePlan(block: RoutePlanSubject.() -> Unit) {
    RoutePlanSubject(this).block()
}
