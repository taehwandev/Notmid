package app.thdev.glassnavlab.core.data.api.notmid

enum class NotmidContentSource {
    Static,
    Api,
    ;

    companion object {
        fun from(value: String): NotmidContentSource {
            return when (value.trim().lowercase()) {
                "static", "fake", "fixture" -> Static
                "api", "remote" -> Api
                else -> error("Unsupported NOTMID_CONTENT_SOURCE: $value")
            }
        }
    }
}
