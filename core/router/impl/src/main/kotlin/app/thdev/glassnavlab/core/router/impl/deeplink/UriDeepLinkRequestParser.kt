package app.thdev.glassnavlab.core.router.impl.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class UriDeepLinkRequestParser : DeepLinkRequestParser {
    override fun parse(uriString: String): DeepLinkRequest? {
        val uri = runCatching { URI(uriString) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null

        return DeepLinkRequest(
            scheme = scheme.lowercase(),
            host = host.lowercase(),
            pathSegments = uri.path
                .orEmpty()
                .split("/")
                .filter(String::isNotBlank),
            queryParameters = uri.rawQuery.orEmpty().toQueryParameters(),
        )
    }
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
