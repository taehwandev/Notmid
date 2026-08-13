package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidPlace

internal data class MapResponse(
    val places: List<NotmidPlace>,
    val highlightedClipIds: List<String>,
)
