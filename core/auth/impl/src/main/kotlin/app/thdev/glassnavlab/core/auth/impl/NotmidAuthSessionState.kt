package app.thdev.glassnavlab.core.auth.impl

import app.thdev.glassnavlab.core.model.notmid.NotmidAuthSession
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class NotmidAuthSessionState(initial: NotmidAuthState) {
    private val mutable = MutableStateFlow(initial)
    val states = mutable.asStateFlow()
    var value: NotmidAuthState
        get() = mutable.value
        set(value) { mutable.value = value }

    fun applyProfileUpdate(expectedSession: NotmidAuthSession, user: NotmidAuthUser): NotmidAuthState {
        mutable.update { current ->
            // A new login can have equal credentials (notably local mode) but is a new session.
            if (current.session === expectedSession && user.id == expectedSession.user.id) {
                current.copy(session = expectedSession.copy(user = user))
            } else current
        }
        return mutable.value
    }
}
