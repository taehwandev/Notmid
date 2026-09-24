package app.thdev.glassnavlab.core.data.api.notmid

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError

sealed class ApiNotmidContentException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    class HttpStatus(
        val path: String,
        val statusCode: Int,
        val body: String,
    ) : ApiNotmidContentException(
        "notmid API request failed for $path with HTTP $statusCode.",
    )

    class Network(
        val path: String,
        val error: NotmidNetworkError,
    ) : ApiNotmidContentException(
        "notmid API network request failed for $path: ${error.message}",
    )

    class MalformedJson(
        val path: String,
        cause: Throwable,
    ) : ApiNotmidContentException(
        "notmid API response for $path did not match the expected contract.",
        cause,
    )
}
