package app.thdev.glassnavlab.core.navigation.assertions

import app.thdev.glassnavlab.core.navigation.route.ComposeRoute

data class TestComposeRoute(
    override val route: String,
) : ComposeRoute
