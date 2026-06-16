package app.thdev.glassnavlab.core.network.notmid

interface NotmidNetworkClient {
    suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse
}

data class NotmidNetworkRequest(
    val method: NotmidHttpMethod,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

enum class NotmidHttpMethod {
    Get,
    Post,
    Patch,
}

data class NotmidNetworkResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, List<String>>,
) {
    val isSuccessful: Boolean
        get() = statusCode in 200..299
}

data class NotmidNetworkError(
    val code: NotmidNetworkErrorCode,
    val message: String,
    val causeName: String? = null,
)

class NotmidNetworkException(
    val error: NotmidNetworkError,
) : RuntimeException(error.message)

enum class NotmidNetworkErrorCode {
    InvalidRequest,
    Transport,
    Timeout,
}
