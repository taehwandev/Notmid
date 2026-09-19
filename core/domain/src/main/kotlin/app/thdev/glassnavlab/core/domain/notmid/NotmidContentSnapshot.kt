package app.thdev.glassnavlab.core.domain.notmid

import app.thdev.glassnavlab.core.model.notmid.NotmidDestination

sealed interface NotmidContentSnapshot {
    data object Loading : NotmidContentSnapshot
    data class Ready(val destinations: List<NotmidDestination>) : NotmidContentSnapshot
    data object Unavailable : NotmidContentSnapshot
}
