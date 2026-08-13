package app.thdev.glassnavlab.feature.inbox

import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

internal sealed interface ChatAttachmentUi {
    data class Clip(val clip: NotmidClip) : ChatAttachmentUi
    data class Place(val place: NotmidPlace) : ChatAttachmentUi
    data class RoutePlan(val title: String, val description: String) : ChatAttachmentUi
}
