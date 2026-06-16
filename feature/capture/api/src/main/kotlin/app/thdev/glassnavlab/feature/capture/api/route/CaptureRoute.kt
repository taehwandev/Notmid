package app.thdev.glassnavlab.feature.capture.api.route

import app.thdev.glassnavlab.feature.notmid.api.destination.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.route.NotmidTopLevelRoute

object CaptureRoute : NotmidTopLevelRoute {
    override val route: String = "notmid/capture"
    override val selectedDestinationId: String = NotmidDestinationIds.CAPTURE
    override val title: String = "Capture"
    override val deepLinkPathSegments: List<String> = listOf(NotmidDestinationIds.CAPTURE)
}
