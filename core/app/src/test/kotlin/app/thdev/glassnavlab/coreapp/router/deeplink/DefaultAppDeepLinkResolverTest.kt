package app.thdev.glassnavlab.coreapp.router.deeplink

import app.thdev.glassnavlab.core.router.assertions.TestComposeRoute
import app.thdev.glassnavlab.core.router.deeplink.DeepLinkResolver
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.RouteStack
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppDeepLinkResolverTest {
    private val detailRoute = TestComposeRoute("detail")
    private val resolver = DefaultAppDeepLinkResolver(
        resolver = DeepLinkResolver { uriString ->
            if (uriString == "https://example.test/app/detail") {
                RoutePlan.compose(RouteStack.single(detailRoute))
            } else {
                null
            }
        },
    )

    @Test
    fun delegatesToCoreDeepLinkResolver() {
        val plan = resolver.resolve("https://example.test/app/detail")

        assertEquals(
            listOf(detailRoute),
            plan?.composeStack?.entries,
        )
    }
}
