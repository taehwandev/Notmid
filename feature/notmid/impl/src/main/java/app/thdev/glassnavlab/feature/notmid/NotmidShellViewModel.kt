package app.thdev.glassnavlab.feature.notmid

import androidx.lifecycle.ViewModel
import app.thdev.glassnavlab.core.navigation.runtime.RouteEventSink
import app.thdev.glassnavlab.core.navigation.notmid.NotmidRouteEvent
import app.thdev.glassnavlab.feature.inbox.api.event.InboxRouteEvent
import app.thdev.glassnavlab.feature.map.api.event.MapRouteEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class NotmidShellViewModel @Inject constructor(private val routeEvents: RouteEventSink) : ViewModel() {
    fun onAction(action: NotmidShellAction) {
        val event = when (action) {
            is NotmidShellAction.DestinationClicked -> NotmidRouteEvent.DestinationSelected(action.destinationId)
            is NotmidShellAction.ThreadClicked -> InboxRouteEvent.ChatThreadRequested(action.threadId)
            is NotmidShellAction.PlaceClicked -> MapRouteEvent.PlaceRequested(action.placeId)
            NotmidShellAction.SettingsClicked -> NotmidRouteEvent.SettingsRequested
        }
        routeEvents.onRouteEvent(event)
    }
}
