package app.thdev.glassnavlab.feature.capture

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** ViewModel-owned commands executed by the lifecycle-bound Android adapter. */
@ViewModelScoped
internal class CapturePlatformRequests @Inject constructor() {
    enum class Request { Permission, Photo }
    private val channel = Channel<Request>(Channel.BUFFERED)
    val requests = channel.receiveAsFlow()
    suspend fun send(request: Request) = channel.send(request)
    fun close() = channel.close()
}
