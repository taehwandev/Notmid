package app.thdev.glassnavlab.core.network.assertions

object ApiErrorEnvelopeFixtures {
    fun body(
        code: String,
        message: String? = null,
    ): String {
        return buildString {
            append("""{"error":{"code":"""")
            append(code)
            append('"')
            if (message != null) {
                append(""","message":"""")
                append(message)
                append('"')
            }
            append("}}")
        }
    }

    val AuthRequired: String = body(code = "auth_required")
}
