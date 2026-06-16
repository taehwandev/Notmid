package app.thdev.glassnavlab.core.model.notmid

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel-facing input stream for ordered UI actions.
 *
 * Unlike one-shot UI effects, actions should not be dropped before the
 * ViewModel reducer boundary can handle them.
 */
interface NotmidActionDelegate<Action> {
    val actions: Flow<Action>

    fun tryDispatch(action: Action): Boolean

    suspend fun dispatch(action: Action)

    fun close()
}

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
