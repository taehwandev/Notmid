package app.thdev.glassnavlab.core.domain.notmid

import kotlinx.coroutines.flow.StateFlow

/** Observes repository reads without initiating a second request. */
interface NotmidContentUpdates {
    val snapshot: StateFlow<NotmidContentSnapshot>
}
