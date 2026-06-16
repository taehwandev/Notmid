package app.thdev.glassnavlab.core.router.deeplink

data class DeepLinkRequest(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
    val queryParameters: Map<String, List<String>>,
) {
    fun pathSegmentsAfter(prefix: String): List<String>? {
        if (pathSegments.firstOrNull() != prefix) return null
        return pathSegments.drop(1)
    }

    fun pathSegmentsAfter(prefix: List<String>): List<String>? {
        if (prefix.isEmpty()) return pathSegments
        if (pathSegments.size < prefix.size) return null
        if (pathSegments.take(prefix.size) != prefix) return null
        return pathSegments.drop(prefix.size)
    }

    fun queryParameter(name: String): String? {
        return queryParameters[name]?.firstOrNull()
    }
}
