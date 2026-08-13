package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidContentRepository

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
