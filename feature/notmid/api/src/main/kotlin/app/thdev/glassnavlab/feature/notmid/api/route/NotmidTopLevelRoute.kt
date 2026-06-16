package app.thdev.glassnavlab.feature.notmid.api.route

import app.thdev.glassnavlab.core.router.route.TopLevelRoute

interface NotmidTopLevelRoute : NotmidRoute, TopLevelRoute {
    override val destinationId: String
        get() = selectedDestinationId
}
