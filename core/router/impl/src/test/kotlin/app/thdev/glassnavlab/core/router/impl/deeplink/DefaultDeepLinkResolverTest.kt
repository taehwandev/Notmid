package app.thdev.glassnavlab.core.router.impl.deeplink

import app.thdev.glassnavlab.core.router.assertions.TestComposeRoute
import app.thdev.glassnavlab.core.router.assertions.TestTopLevelRoute
import app.thdev.glassnavlab.core.router.assertions.assertRoutePlan
import app.thdev.glassnavlab.core.router.impl.registry.DefaultRouteRegistry
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultDeepLinkResolverTest {
    private val homeRoute = TestTopLevelRoute("home")
    private val detailRoute = TestComposeRoute("detail")
    private val registry = DefaultRouteRegistry(
        defaultRoute = homeRoute,
        topLevelRoutes = listOf(homeRoute),
        deepLinkSpecs = listOf(
            StaticRouteDeepLinkSpec(
                pathSegments = listOf("detail"),
                route = detailRoute,
            ),
        ),
    )
    private val resolver = DefaultDeepLinkResolver(
        policy = DeepLinkUrlPolicy.withBasePath(
            scheme = "https",
            host = "example.test",
            basePath = "app",
        ),
        routeRegistry = registry,
    )

    @Test
    fun stripsBasePathBeforeRegistryResolution() {
        val plan = resolver.resolve("https://example.test/app/detail")

        plan?.assertRoutePlan {
            hasComposeStack(detailRoute)
            hasNoActivityRoutes()
        }
    }

    @Test
    fun emptyBasePathResolvesDefaultStack() {
        val plan = resolver.resolve("https://example.test/app")

        assertEquals(
            listOf(homeRoute),
            plan?.composeStack?.entries,
        )
    }

    @Test
    fun rejectsWrongHost() {
        val plan = resolver.resolve("https://other.test/app/detail")

        assertNull(plan)
    }

    @Test
    fun rejectsWrongBasePath() {
        val plan = resolver.resolve("https://example.test/other/detail")

        assertNull(plan)
    }

    @Test
    fun prefixSpecUsesRemainingSegmentsForDynamicPlan() {
        val childRoute = TestComposeRoute("child")
        val prefixRegistry = DefaultRouteRegistry(
            defaultRoute = homeRoute,
            topLevelRoutes = listOf(homeRoute),
            deepLinkSpecs = listOf(
                PrefixRouteDeepLinkSpec(
                    prefixSegments = listOf("items"),
                    planFactory = { remainingPathSegments, _ ->
                        if (remainingPathSegments == listOf("123")) {
                            RoutePlan.compose(RouteStack.single(childRoute))
                        } else {
                            null
                        }
                    },
                ),
            ),
        )
        val prefixResolver = DefaultDeepLinkResolver(
            policy = DeepLinkUrlPolicy.singleHost(
                scheme = "https",
                host = "example.test",
            ),
            routeRegistry = prefixRegistry,
        )

        val plan = prefixResolver.resolve("https://example.test/items/123")

        plan?.assertRoutePlan {
            hasComposeStack(childRoute)
            hasNoActivityRoutes()
        }
    }
}
