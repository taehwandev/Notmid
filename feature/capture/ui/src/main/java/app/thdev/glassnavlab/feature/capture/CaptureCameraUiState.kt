package app.thdev.glassnavlab.feature.capture

internal data class CaptureCameraUiState(
    val mode: CaptureCameraMode,
    val lens: CaptureCameraLens,
    val permissionGranted: Boolean,
    val status: CaptureCameraStatus,
)
