package app.thdev.glassnavlab.core.network.notmid

class NotmidNetworkException(
    val error: NotmidNetworkError,
) : RuntimeException(error.message)
