package app.thdev.glassnavlab.core.domain.notmid

sealed class NotmidProtectedWriteFailure(
    val action: NotmidProtectedWriteAction,
) {
    class MissingAuth(
        action: NotmidProtectedWriteAction,
    ) : NotmidProtectedWriteFailure(action)

    class InvalidRequest(
        action: NotmidProtectedWriteAction,
        val code: String,
        val message: String,
    ) : NotmidProtectedWriteFailure(action)

    class HttpStatus(
        action: NotmidProtectedWriteAction,
        val path: String,
        val statusCode: Int,
        val body: String,
    ) : NotmidProtectedWriteFailure(action)

    class Network(
        action: NotmidProtectedWriteAction,
        val path: String,
        val code: String,
        val message: String,
        val causeName: String?,
    ) : NotmidProtectedWriteFailure(action)

    class MalformedResponse(
        action: NotmidProtectedWriteAction,
        val path: String,
        val causeName: String?,
    ) : NotmidProtectedWriteFailure(action)
}
