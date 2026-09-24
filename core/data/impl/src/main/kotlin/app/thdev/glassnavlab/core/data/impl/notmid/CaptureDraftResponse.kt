package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace

internal data class CaptureDraftResponse(
    val draft: NotmidCaptureDraft,
    val candidatePlaces: List<NotmidPlace>,
)
