package app.thdev.glassnavlab.core.network.notmid

object NotmidApiPaths {
    const val HEALTH = "/health"
    const val AUTH_STATUS = "/v1/auth/status"
    const val AUTH_FAKE_SIGN_IN = "/v1/auth/fake-sign-in"
    const val CAPTURE_DRAFT = "/v1/capture/draft"
    const val CAPTURE_PUBLISH = "/v1/capture/publish"
    const val FEED = "/v1/feed"
    const val MAP = "/v1/map"
    const val INBOX_THREADS = "/v1/inbox/threads"
    const val PROFILE_SETTINGS = "/v1/profile/settings"
    const val DEEPLINK_RESOLVE = "/v1/deeplinks/resolve"

    fun clip(clipId: String): String = "/v1/clips/${clipId.urlPathSegment()}"

    fun clipSave(clipId: String): String = "${clip(clipId)}/save"

    fun place(placeId: String): String = "/v1/places/${placeId.urlPathSegment()}"

    fun thread(threadId: String): String = "/v1/inbox/threads/${threadId.urlPathSegment()}"

    fun threadDetail(threadId: String): String = "${thread(threadId)}/detail"

    fun threadMessages(threadId: String): String = "${thread(threadId)}/messages"

    fun threadInviteAccept(threadId: String): String = "${thread(threadId)}/invite/accept"

    fun threadInviteReject(threadId: String): String = "${thread(threadId)}/invite/reject"
}

private fun String.urlPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
