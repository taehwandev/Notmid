package app.thdev.glassnavlab.core.model.notmid

import kotlinx.coroutines.flow.Flow

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
