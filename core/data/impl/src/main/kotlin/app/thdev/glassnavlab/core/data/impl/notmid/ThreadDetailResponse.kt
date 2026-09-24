package app.thdev.glassnavlab.core.data.impl.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidClip
import app.thdev.glassnavlab.core.model.notmid.NotmidPlace
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage

internal data class ThreadDetailResponse(
    val thread: NotmidThread,
    val messages: List<NotmidThreadMessage>,
    val attachedClip: NotmidClip?,
    val attachedPlace: NotmidPlace?,
)
