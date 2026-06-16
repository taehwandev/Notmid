package app.thdev.glassnavlab.navigation

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class AppDeepLinkResolver {
    fun resolve(uriString: String): RoutePlan? {
        val request = uriString.toDeepLinkRequest() ?: return null
        if (request.scheme != APP_WEB_SCHEME || request.host != APP_WEB_HOST) return null

        val featurePathSegments = request.pathSegmentsAfter(APP_WEB_BASE_PATH) ?: return null
        return NotmidRouteGraph.resolveDeepLink(
            request.copy(pathSegments = featurePathSegments),
        )
    }

    companion object {
        const val APP_WEB_SCHEME = "https"
        const val APP_WEB_HOST = "thdev.app"
        const val APP_WEB_BASE_PATH = "notmid"
    }
}

private fun String.toDeepLinkRequest(): DeepLinkRequest? {
    val uri = runCatching { URI(this) }.getOrNull() ?: return null
    val scheme = uri.scheme ?: return null
    val host = uri.host ?: return null
    val pathSegments = uri.path
        .orEmpty()
        .split("/")
        .filter(String::isNotBlank)

    return DeepLinkRequest(
        scheme = scheme.lowercase(),
        host = host.lowercase(),
        pathSegments = pathSegments,
        queryParameters = uri.query.orEmpty().toQueryParameters(),
    )
}

private fun String.toQueryParameters(): Map<String, List<String>> {
    if (isBlank()) return emptyMap()

    return split("&")
        .mapNotNull { pair ->
            val key = pair.substringBefore("=", missingDelimiterValue = "")
                .takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val value = pair.substringAfter("=", missingDelimiterValue = "")
            key.urlDecode() to value.urlDecode()
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )
}

private fun String.urlDecode(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.name())
}
