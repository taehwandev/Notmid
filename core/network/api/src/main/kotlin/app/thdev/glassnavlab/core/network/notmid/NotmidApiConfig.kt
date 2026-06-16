package app.thdev.glassnavlab.core.network.notmid

data class NotmidApiConfig(
    val baseUrl: String,
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank." }
    }

    fun urlFor(path: String): String {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = path.trimStart('/')
        return "$normalizedBase/$normalizedPath"
    }
}
