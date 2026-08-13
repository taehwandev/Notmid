package app.thdev.glassnavlab.core.model.notmid

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class ChannelNotmidActionDelegate<Action>(
    capacity: Int = Channel.BUFFERED,
) : NotmidActionDelegate<Action> {
    private val actionChannel = Channel<Action>(capacity)

    override val actions: Flow<Action> = actionChannel.receiveAsFlow()

    override fun tryDispatch(action: Action): Boolean {
        return actionChannel.trySend(action).isSuccess
    }

    override suspend fun dispatch(action: Action) {
        actionChannel.send(action)
    }

    override fun close() {
        actionChannel.close()
    }
}
