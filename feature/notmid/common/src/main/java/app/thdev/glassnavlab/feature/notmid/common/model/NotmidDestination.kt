package app.thdev.glassnavlab.feature.notmid.common.model

import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureDraft
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage

data class NotmidDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: NotmidNavigationIcon,
    val clips: List<NotmidClip>,
    val places: List<NotmidPlace>,
    val threads: List<NotmidThread>,
    val captureDraft: NotmidCaptureDraft?,
    val threadMessages: List<NotmidThreadMessage> = emptyList(),
)
