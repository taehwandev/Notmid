package app.thdev.glassnavlab.core.network.notmid

data class NotmidNetworkResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, List<String>>,
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299
}
