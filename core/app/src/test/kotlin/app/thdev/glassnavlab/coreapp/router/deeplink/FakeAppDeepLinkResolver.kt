package app.thdev.glassnavlab.coreapp.router.deeplink

import app.thdev.glassnavlab.core.router.runtime.RoutePlan

class FakeAppDeepLinkResolver(
    private val plansByUri: Map<String, RoutePlan> = emptyMap(),
) : AppDeepLinkResolver {
    val requestedUris: List<String>
        get() = mutableRequestedUris.toList()

    private val mutableRequestedUris = mutableListOf<String>()

    override fun resolve(uriString: String): RoutePlan? {
        mutableRequestedUris += uriString
        return plansByUri[uriString]
    }
}
