package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

sealed interface QueuedNetworkResult {
    fun responseOrThrow(): NotmidNetworkResponse
}
