package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidThread
import app.thdev.glassnavlab.core.model.notmid.NotmidThreadMessage
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser

/** Data changes a caller may apply after a successful protected write. */
sealed interface NotmidProtectedWriteResult {
    data object Completed : NotmidProtectedWriteResult

    data class MessageSent(val message: NotmidThreadMessage) : NotmidProtectedWriteResult

    data class ThreadStarted(
        val thread: NotmidThread,
        val message: NotmidThreadMessage?,
    ) : NotmidProtectedWriteResult

    data class ThreadUpdated(val thread: NotmidThread) : NotmidProtectedWriteResult

    data class ProfileUpdated(val user: NotmidAuthUser) : NotmidProtectedWriteResult
}
