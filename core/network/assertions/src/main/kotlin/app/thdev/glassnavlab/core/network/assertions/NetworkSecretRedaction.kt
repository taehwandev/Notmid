package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest

private const val RedactedValue = "<redacted>"

private val DefaultSecretHeaderNames = setOf(
    "authorization",
    "cookie",
    "set-cookie",
    "x-api-key",
)

fun NotmidNetworkRequest.redactedHeaders(
    secretHeaderNames: Set<String> = DefaultSecretHeaderNames,
): Map<String, String> {
    val lowerSecretNames = secretHeaderNames.mapTo(mutableSetOf()) { headerName ->
        headerName.lowercase()
    }
    return headers.mapValues { (name, value) ->
        if (name.lowercase() in lowerSecretNames) RedactedValue else value
    }
}
