package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

data class QueuedNetworkResponse(
    val body: String,
    val statusCode: Int = 200,
    val headers: Map<String, List<String>> = emptyMap(),
) : QueuedNetworkResult {
    override fun responseOrThrow(): NotmidNetworkResponse {
        return NotmidNetworkResponse(
            statusCode = statusCode,
            body = body,
            headers = headers,
        )
    }
}
