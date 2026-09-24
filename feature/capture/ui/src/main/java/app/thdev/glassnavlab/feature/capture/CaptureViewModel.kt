package app.thdev.glassnavlab.feature.capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentSnapshot
import app.thdev.glassnavlab.core.domain.notmid.NotmidContentUpdates
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidActionDelegate
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureMediaState
import app.thdev.glassnavlab.core.model.notmid.NotmidCapturePublishRequest
import app.thdev.glassnavlab.core.model.notmid.NotmidCaptureVisibility
import app.thdev.glassnavlab.feature.capture.api.route.CaptureRoute
import app.thdev.glassnavlab.feature.notmid.common.model.toNotmidDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class CaptureViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val content: NotmidContentUpdates,
    private val writes: NotmidActionDelegate<NotmidProtectedWriteRequest>,
    private val platform: CapturePlatformRequests,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CaptureUiState(camera = CaptureCameraUiState(
        mode = saved.get<String>("mode")?.let(CaptureCameraMode::valueOf) ?: CaptureCameraMode.Camera,
        lens = saved.get<String>("lens")?.let(CaptureCameraLens::valueOf) ?: CaptureCameraLens.Back,
        permissionGranted = false,
        status = CaptureCameraStatus.PermissionRequired,
    )))
    val state = mutableState.asStateFlow()
    val platformRequests = platform.requests

    init {
        viewModelScope.launch { content.snapshot.collect { refreshContent(it) } }
    }

    private fun refreshContent(snapshot: NotmidContentSnapshot) {
        if (snapshot !is NotmidContentSnapshot.Ready) return
        val destinations = snapshot.destinations.toNotmidDestinations()
        val destination = destinations.firstOrNull { it.id == CaptureRoute.selectedDestinationId }
            ?: destinations.firstOrNull()
        val draft = destination?.captureDraft
        var next = state.value.copy(destination = destination)
        if (state.value.destination == null || state.value.destination?.captureDraft?.id != draft?.id) {
            val restore = saved.get<String>("draftId") == draft?.id
            next = next.copy(
                caption = if (restore) saved["caption"] ?: draft?.caption.orEmpty() else draft?.caption.orEmpty(),
                selectedTags = if (restore) saved.get<ArrayList<String>>("tags")?.toSet() ?: draft?.moodTags.orEmpty().toSet() else draft?.moodTags.orEmpty().toSet(),
                publicReceipt = if (restore) saved["public"] ?: (draft?.visibility != NotmidCaptureVisibility.Private) else draft?.visibility != NotmidCaptureVisibility.Private,
                draftStatus = if (restore) saved["status"] ?: draft?.statusLabel ?: "Draft saved locally" else draft?.statusLabel ?: "Draft saved locally",
                capturedMediaName = if (restore) saved["media"] else null,
            )
        }
        update(next)
    }

    fun onAction(action: CaptureAction) {
        val current = state.value
        when (action) {
            is CaptureAction.CaptionChanged -> update(current.copy(caption = action.value))
            is CaptureAction.TagSelected -> update(current.copy(selectedTags = if (action.value in current.selectedTags) current.selectedTags - action.value else current.selectedTags + action.value))
            is CaptureAction.VisibilityChanged -> update(current.copy(publicReceipt = action.public))
            is CaptureAction.ModeChanged -> update(current.copy(camera = current.camera.copy(
                mode = action.mode,
                status = if (action.mode == CaptureCameraMode.Upload) current.camera.status else if (current.camera.permissionGranted) CaptureCameraStatus.Starting else CaptureCameraStatus.PermissionRequired,
            )))
            is CaptureAction.LensChanged -> update(current.copy(camera = current.camera.copy(lens = action.lens)))
            is CaptureAction.PermissionChanged -> update(current.copy(camera = current.camera.copy(
                permissionGranted = action.granted,
                status = if (!action.granted) CaptureCameraStatus.PermissionRequired else if (!current.camera.permissionGranted) CaptureCameraStatus.Starting else current.camera.status,
            )))
            is CaptureAction.CameraStatusChanged -> update(current.copy(camera = current.camera.copy(status = action.status)))
            is CaptureAction.PhotoCaptured -> update(current.copy(capturedMediaName = action.fileName, draftStatus = "Photo captured locally", camera = current.camera.copy(status = CaptureCameraStatus.Previewing)))
            CaptureAction.PhotoFailed -> update(current.copy(draftStatus = "Camera capture failed", camera = current.camera.copy(status = CaptureCameraStatus.Failed)))
            CaptureAction.RequestPermission -> request(CapturePlatformRequests.Request.Permission)
            CaptureAction.TakePhoto -> if (current.camera.permissionGranted) request(CapturePlatformRequests.Request.Photo)
            CaptureAction.SaveDraft -> update(current.copy(draftStatus = "Draft updated just now"))
            CaptureAction.Publish -> publish()
        }
    }

    private fun update(value: CaptureUiState) {
        val draft = value.destination?.captureDraft
        val place = value.destination?.places?.firstOrNull { it.id == draft?.placeId }
            ?: value.destination?.places?.firstOrNull()
        val media = value.capturedMediaName != null || draft?.mediaState?.let { it != NotmidCaptureMediaState.Empty } == true
        mutableState.value = value.copy(attachedPlace = place, readyToPublish = draft != null && media && value.caption.isNotBlank() && value.selectedTags.isNotEmpty() && place != null)
        saved["mode"] = value.camera.mode.name
        saved["lens"] = value.camera.lens.name
        if (value.destination == null) return
        saved["draftId"] = draft?.id
        saved["caption"] = value.caption
        saved["tags"] = ArrayList(value.selectedTags)
        saved["public"] = value.publicReceipt
        saved["status"] = value.draftStatus
        saved["media"] = value.capturedMediaName
    }

    private fun publish() {
        refreshContent(content.snapshot.value)
        val current = state.value
        val draft = current.destination?.captureDraft
        val place = current.attachedPlace
        if (content.snapshot.value !is NotmidContentSnapshot.Ready || !current.readyToPublish || draft == null || place == null) {
            update(current.copy(draftStatus = "Attach media, a place, caption, and at least one tag"))
            return
        }
        update(current.copy(draftStatus = "Publishing receipt..."))
        val request = NotmidCapturePublishRequest(draft.id, current.caption, place.id, current.selectedTags.toList(), if (current.publicReceipt) NotmidCaptureVisibility.Public else NotmidCaptureVisibility.Private)
        viewModelScope.launch { writes.dispatch(NotmidProtectedWriteRequest.PublishCapture(request)) }
    }

    private fun request(request: CapturePlatformRequests.Request) {
        viewModelScope.launch { platform.send(request) }
    }

    override fun onCleared() { platform.close() }
}
