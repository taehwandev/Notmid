package app.thdev.glassnavlab.feature.capture

import androidx.compose.runtime.Immutable
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Immutable
internal data class CaptureUiState(
    val destination: NotmidDestination? = null,
    val caption: String = "",
    val selectedTags: Set<String> = emptySet(),
    val publicReceipt: Boolean = true,
    val draftStatus: String = "Draft saved locally",
    val capturedMediaName: String? = null,
    val attachedPlace: NotmidPlace? = null,
    val readyToPublish: Boolean = false,
    val camera: CaptureCameraUiState = CaptureCameraUiState(
        CaptureCameraMode.Camera, CaptureCameraLens.Back, false, CaptureCameraStatus.PermissionRequired,
    ),
)
