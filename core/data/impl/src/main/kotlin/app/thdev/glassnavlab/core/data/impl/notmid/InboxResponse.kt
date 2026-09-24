package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidThread

internal data class InboxResponse(
    val threads: List<NotmidThread>,
)
