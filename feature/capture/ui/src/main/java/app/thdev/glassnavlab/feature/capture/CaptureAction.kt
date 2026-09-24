package app.thdev.glassnavlab.feature.capture

internal sealed interface CaptureAction {
    data class CaptionChanged(val value: String) : CaptureAction
    data class TagSelected(val value: String) : CaptureAction
    data class VisibilityChanged(val public: Boolean) : CaptureAction
    data class ModeChanged(val mode: CaptureCameraMode) : CaptureAction
    data class LensChanged(val lens: CaptureCameraLens) : CaptureAction
    data class PermissionChanged(val granted: Boolean) : CaptureAction
    data class CameraStatusChanged(val status: CaptureCameraStatus) : CaptureAction
    data class PhotoCaptured(val fileName: String) : CaptureAction
    data object PhotoFailed : CaptureAction
    data object RequestPermission : CaptureAction
    data object TakePhoto : CaptureAction
    data object SaveDraft : CaptureAction
    data object Publish : CaptureAction
}
