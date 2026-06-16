package app.thdev.glassnavlab.core.router.impl.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest

data class DeepLinkUrlPolicy(
    val allowedSchemes: Set<String>,
    val allowedHosts: Set<String>,
    val basePathSegments: List<String> = emptyList(),
) {
    init {
        require(allowedSchemes.isNotEmpty()) { "At least one deep-link scheme is required." }
        require(allowedSchemes.none(String::isBlank)) {
            "Deep-link schemes must not contain blank values."
        }
        require(allowedHosts.isNotEmpty()) { "At least one deep-link host is required." }
        require(allowedHosts.none(String::isBlank)) {
            "Deep-link hosts must not contain blank values."
        }
        require(basePathSegments.none(String::isBlank)) {
            "Deep-link base path segments must not contain blank values."
        }
    }

    private val normalizedSchemes = allowedSchemes.mapTo(mutableSetOf()) { scheme ->
        scheme.lowercase()
    }
    private val normalizedHosts = allowedHosts.mapTo(mutableSetOf()) { host ->
        host.lowercase()
    }

    fun normalize(request: DeepLinkRequest): DeepLinkRequest? {
        if (request.scheme.lowercase() !in normalizedSchemes) return null
        if (request.host.lowercase() !in normalizedHosts) return null
        val featurePathSegments = request.pathSegmentsAfter(basePathSegments) ?: return null

        return request.copy(
            scheme = request.scheme.lowercase(),
            host = request.host.lowercase(),
            pathSegments = featurePathSegments,
        )
    }

    companion object {
        fun singleHost(
            scheme: String,
            host: String,
        ): DeepLinkUrlPolicy {
            return DeepLinkUrlPolicy(
                allowedSchemes = setOf(scheme),
                allowedHosts = setOf(host),
            )
        }

        fun withBasePath(
            scheme: String,
            host: String,
            basePath: String,
        ): DeepLinkUrlPolicy {
            return DeepLinkUrlPolicy(
                allowedSchemes = setOf(scheme),
                allowedHosts = setOf(host),
                basePathSegments = basePath
                    .split("/")
                    .filter(String::isNotBlank),
            )
        }
    }
}
