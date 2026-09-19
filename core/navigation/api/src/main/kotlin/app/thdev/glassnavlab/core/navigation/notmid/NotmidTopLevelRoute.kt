package app.thdev.glassnavlab.core.navigation.notmid

import app.thdev.glassnavlab.core.navigation.route.TopLevelRoute

interface NotmidTopLevelRoute : NotmidRoute, TopLevelRoute {
    override val destinationId: String
        get() = selectedDestinationId
}
