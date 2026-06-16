package app.thdev.glassnavlab.core.router.impl.deeplink

import app.thdev.glassnavlab.core.router.deeplink.DeepLinkRequest

fun interface DeepLinkRequestParser {
    fun parse(uriString: String): DeepLinkRequest?
}
