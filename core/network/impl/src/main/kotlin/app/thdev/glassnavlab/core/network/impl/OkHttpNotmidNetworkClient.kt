package app.thdev.glassnavlab.core.network.notmid

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpNotmidNetworkClient(
    private val config: NotmidApiConfig,
    private val connectTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) : NotmidNetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
        .build()

    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        if (request.path.isBlank()) {
            throw NotmidNetworkException(
                NotmidNetworkError(
                    code = NotmidNetworkErrorCode.InvalidRequest,
                    message = "Request path must not be blank.",
                ),
            )
        }

        return try {
            client.newCall(request.toOkHttpRequest(config)).execute().use { response ->
                NotmidNetworkResponse(
                    statusCode = response.code,
                    body = response.body?.string().orEmpty(),
                    headers = response.headers.toMultimap(),
                )
            }
        } catch (exception: SocketTimeoutException) {
            throw exception.toNetworkException(NotmidNetworkErrorCode.Timeout)
        } catch (exception: IOException) {
            throw exception.toNetworkException(NotmidNetworkErrorCode.Transport)
        } catch (exception: IllegalArgumentException) {
            throw exception.toNetworkException(NotmidNetworkErrorCode.InvalidRequest)
        }
    }
}

private val JsonMediaType = "application/json".toMediaType()

private fun NotmidNetworkRequest.toOkHttpRequest(config: NotmidApiConfig): okhttp3.Request {
    val builder = Builder()
        .url(config.urlFor(path))
        .header("accept", "application/json")

    headers.forEach { (name, value) ->
        builder.header(name, value)
    }

    val requestBody = body.orEmpty().toRequestBody(JsonMediaType)
    return when (method) {
        NotmidHttpMethod.Get -> builder.get().build()
        NotmidHttpMethod.Post -> builder.post(requestBody).build()
        NotmidHttpMethod.Patch -> builder.patch(requestBody).build()
    }
}

private fun Exception.toNetworkException(code: NotmidNetworkErrorCode): NotmidNetworkException {
    return NotmidNetworkException(
        NotmidNetworkError(
            code = code,
            message = message ?: "notmid API request failed.",
            causeName = this::class.java.simpleName,
        ),
    )
}

private const val DEFAULT_TIMEOUT_MILLIS = 10_000
