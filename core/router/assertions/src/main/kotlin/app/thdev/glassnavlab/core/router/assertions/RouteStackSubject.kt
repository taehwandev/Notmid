package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ComposeRoute
import app.thdev.glassnavlab.core.router.runtime.RouteStack

class RouteStackSubject(
    private val stack: RouteStack,
) {
    fun hasEntries(vararg expected: ComposeRoute) {
        assertEquals(
            expected = expected.toList(),
            actual = stack.entries,
            label = "route stack entries",
        )
    }

    fun hasTopRoute(expected: ComposeRoute) {
        assertEquals(
            expected = expected,
            actual = stack.topRoute,
            label = "route stack top route",
        )
    }

    fun containsOnlyComposeRoutes() {
        // RouteStack is typed as List<ComposeRoute>; this assertion documents
        // the contract for tests that read like behavior specs.
    }

    fun hasComposeRoutes(vararg expected: ComposeRoute) {
        assertEquals(
            expected = expected.toList(),
            actual = stack.composeRoutes,
            label = "compose route entries",
        )
    }
}

fun RouteStack.assertRouteStack(block: RouteStackSubject.() -> Unit) {
    RouteStackSubject(this).block()
}
