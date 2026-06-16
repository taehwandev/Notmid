package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteFailure
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun NotmidNetworkResponse.toProtectedWriteFailure(
    action: NotmidProtectedWriteAction,
    path: String,
): NotmidProtectedWriteFailure {
    val serverError = body.toApiErrorBody()
    if (serverError != null && statusCode in BusinessErrorStatusCodes) {
        return NotmidProtectedWriteFailure.InvalidRequest(
            action = action,
            code = serverError.code,
            message = serverError.message,
        )
    }

    return NotmidProtectedWriteFailure.HttpStatus(
        action = action,
        path = path,
        statusCode = statusCode,
        body = body,
    )
}

private data class NotmidApiErrorBody(
    val code: String,
    val message: String,
)

private val BusinessErrorStatusCodes = setOf(400, 403, 404, 409, 422)

private fun String.toApiErrorBody(): NotmidApiErrorBody? {
    return runCatching {
        val root = Json.parseToJsonElement(this).jsonObject
        val error = root["error"]?.jsonObject ?: root
        val code = error["code"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val message = error["message"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (code.isBlank() || message.isBlank()) {
            null
        } else {
            NotmidApiErrorBody(
                code = code,
                message = message,
            )
        }
    }.getOrNull()
}
