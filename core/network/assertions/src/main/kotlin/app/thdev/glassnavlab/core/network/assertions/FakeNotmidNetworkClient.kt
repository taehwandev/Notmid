package app.thdev.glassnavlab.core.network.assertions

import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkClient
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkRequest
import app.thdev.glassnavlab.core.network.notmid.NotmidNetworkResponse

class FakeNotmidNetworkClient(
    private val result: QueuedNetworkResult,
) : NotmidNetworkClient {
    override suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse {
        return result.responseOrThrow()
    }
}
