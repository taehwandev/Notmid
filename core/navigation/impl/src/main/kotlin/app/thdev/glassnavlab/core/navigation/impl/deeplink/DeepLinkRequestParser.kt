package app.thdev.glassnavlab.core.navigation.impl.deeplink

import app.thdev.glassnavlab.core.navigation.deeplink.DeepLinkRequest

fun interface DeepLinkRequestParser {
    fun parse(uriString: String): DeepLinkRequest?
}
