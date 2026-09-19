package app.thdev.glassnavlab.core.navigation.impl.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkSpec
import app.thdev.glassnavlab.core.navigation.runtime.RoutePlan

class PrefixRouteDeepLinkSpec(
    private val prefixSegments: List<String>,
    private val planFactory: (remainingPathSegments: List<String>, request: DeepLinkRequest) -> RoutePlan?,
    override val priority: Int = 0,
) : DeepLinkSpec {
    init {
        require(prefixSegments.isNotEmpty()) {
            "Prefix route deep-link path must contain at least one segment."
        }
        require(prefixSegments.none(String::isBlank)) {
            "Prefix route deep-link path must not contain blank segments."
        }
    }

    override fun match(request: DeepLinkRequest): RoutePlan? {
        val remainingPathSegments = request.pathSegmentsAfter(prefixSegments) ?: return null
        return planFactory(remainingPathSegments, request)
    }
}
