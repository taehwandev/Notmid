package app.thdev.glassnavlab.core.runtime.router.config

import app.thdev.glassnavlab.core.router.assertions.TestComposeRoute
import app.thdev.glassnavlab.core.router.assertions.TestRouteEvent
import app.thdev.glassnavlab.core.router.assertions.TestTopLevelRoute
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.router.runtime.RouteEventHandler
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppRouterBundleTest {
    private val homeRoute = TestTopLevelRoute("home")
    private val detailRoute = TestComposeRoute("detail")

    @Test
    fun createsRuntimeWithDefaultStack() {
        val runtime = createBundle().createRuntime()

        assertEquals(
            listOf(homeRoute),
            runtime.backStack.entries,
        )
    }

    @Test
    fun configuresEventHandlers() {
        val runtime = createBundle().createRuntime()

        runtime.onRouteEvent(TestRouteEvent("open-detail"))

        assertEquals(
            listOf(detailRoute),
            runtime.backStack.entries,
        )
    }

    @Test
    fun configuresDeepLinkResolution() {
        val runtime = createBundle().createRuntime()

        runtime.navigateDeepLink("https://example.test/app/detail")

        assertEquals(
            listOf(detailRoute),
            runtime.backStack.entries,
        )
    }

    @Test
    fun supportsDeepLinksWithoutRouteEventHandlers() {
        val runtime = createBundle(routeEventHandlers = emptyList()).createRuntime()

        runtime.navigateDeepLink("https://example.test/app/detail")

        assertEquals(
            listOf(detailRoute),
            runtime.backStack.entries,
        )
    }

    private fun createBundle(
        routeEventHandlers: List<RouteEventHandler> = listOf(
            RouteEventHandler { event ->
                if (event == TestRouteEvent("open-detail")) {
                    RoutePlan.compose(RouteStack.single(detailRoute))
                } else {
                    null
                }
            },
        ),
    ): DefaultAppRouterBundle {
        return DefaultAppRouterBundle(
            config = AppRouterBundleConfig(
                defaultRoute = homeRoute,
                topLevelRoutes = listOf(homeRoute),
                deepLinkSpecs = listOf(
                    object : DeepLinkSpec {
                        override fun match(request: DeepLinkRequest): RoutePlan? {
                            if (request.pathSegments != listOf("detail")) return null
                            return RoutePlan.compose(RouteStack.single(detailRoute))
                        }
                    },
                ),
                deepLinkUrlConfig = AppDeepLinkUrlConfig.withBasePath(
                    scheme = "https",
                    host = "example.test",
                    basePath = "app",
                ),
                routeEventHandlers = routeEventHandlers,
            ),
        )
    }
}
