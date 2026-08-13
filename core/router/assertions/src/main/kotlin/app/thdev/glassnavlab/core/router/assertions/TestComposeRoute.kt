package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.route.ComposeRoute

data class TestComposeRoute(
    override val route: String,
) : ComposeRoute
