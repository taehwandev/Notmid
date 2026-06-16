package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository

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

class NotmidContentRepositorySelector(
    private val staticRepositoryFactory: () -> NotmidContentRepository,
    private val apiRepositoryFactory: () -> NotmidContentRepository,
) {
    fun select(source: NotmidContentSource): NotmidContentRepository {
        return when (source) {
            NotmidContentSource.Static -> staticRepositoryFactory()
            NotmidContentSource.Api -> apiRepositoryFactory()
        }
    }
}
