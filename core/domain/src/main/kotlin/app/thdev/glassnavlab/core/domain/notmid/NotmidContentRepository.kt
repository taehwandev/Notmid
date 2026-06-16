package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

interface NotmidContentRepository {
    suspend fun destinations(): List<NotmidDestination>
}
