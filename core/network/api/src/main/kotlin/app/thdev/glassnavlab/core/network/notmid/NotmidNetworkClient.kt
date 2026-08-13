package app.thdev.glassnavlab.core.network.notmid

interface NotmidNetworkClient {
    suspend fun execute(request: NotmidNetworkRequest): NotmidNetworkResponse
}
