package app.thdev.glassnavlab.core.network.notmid

data class NotmidNetworkError(
    val code: NotmidNetworkErrorCode,
    val message: String,
    val causeName: String? = null,
)
