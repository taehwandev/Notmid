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
}
