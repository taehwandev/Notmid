package app.thdev.glassnavlab.core.runtime.router.config

data class AppDeepLinkUrlConfig(
    val allowedSchemes: Set<String>,
    val allowedHosts: Set<String>,
    val basePathSegments: List<String> = emptyList(),
) {
    init {
        require(allowedSchemes.isNotEmpty()) { "At least one deep-link scheme is required." }
        require(allowedHosts.isNotEmpty()) { "At least one deep-link host is required." }
        require(allowedSchemes.none(String::isBlank)) {
            "Deep-link schemes must not contain blank values."
        }
        require(allowedHosts.none(String::isBlank)) {
            "Deep-link hosts must not contain blank values."
        }
        require(basePathSegments.none(String::isBlank)) {
            "Deep-link base path segments must not contain blank values."
        }
    }

    companion object {
        fun singleHost(
            scheme: String,
            host: String,
        ): AppDeepLinkUrlConfig {
            return AppDeepLinkUrlConfig(
                allowedSchemes = setOf(scheme),
                allowedHosts = setOf(host),
            )
        }

        fun withBasePath(
            scheme: String,
            host: String,
            basePath: String,
        ): AppDeepLinkUrlConfig {
            return AppDeepLinkUrlConfig(
                allowedSchemes = setOf(scheme),
                allowedHosts = setOf(host),
                basePathSegments = basePath
                    .split("/")
                    .filter(String::isNotBlank),
            )
        }
    }
}
