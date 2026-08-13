package app.thdev.glassnavlab.core.network.notmid

data class NotmidNetworkRequest(
    val method: NotmidHttpMethod,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)
