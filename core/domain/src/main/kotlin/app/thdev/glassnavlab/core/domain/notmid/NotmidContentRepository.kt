package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

interface NotmidContentRepository {
    fun destinations(): List<NotmidDestination>
}
