package app.thdev.glassnavlab.feature.inbox

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteAction
import app.thdev.glassnavlab.feature.notmid.notice.NotmidProtectedActionNotice

internal data class ChatWriteUiState(
    val inFlight: NotmidProtectedWriteAction? = null,
    val notice: NotmidProtectedActionNotice? = null,
) {
    fun messageFor(action: NotmidProtectedWriteAction): String? = notice?.takeIf { it.action == action }?.message
}
