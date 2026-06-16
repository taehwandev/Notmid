package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

sealed interface QueuedNetworkResult {
    fun responseOrThrow(): NotmidNetworkResponse
}

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

data class QueuedNetworkFailure(
    val error: NotmidNetworkError,
) : QueuedNetworkResult {
    override fun responseOrThrow(): NotmidNetworkResponse {
        throw NotmidNetworkException(error)
    }
}
