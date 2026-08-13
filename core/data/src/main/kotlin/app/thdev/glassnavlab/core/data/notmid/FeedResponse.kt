package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace

internal data class FeedResponse(
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
)
