package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkError
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkException
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

data class QueuedNetworkFailure(
    val error: NotmidNetworkError,
) : QueuedNetworkResult {
    override fun responseOrThrow(): NotmidNetworkResponse {
        throw NotmidNetworkException(error)
    }
}
