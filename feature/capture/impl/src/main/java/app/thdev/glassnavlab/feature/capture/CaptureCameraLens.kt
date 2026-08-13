package app.thdev.glassnavlab.feature.capture

import androidx.camera.core.CameraSelector

internal enum class CaptureCameraLens(
    val label: String,
    val lensFacing: Int,
) {
    Back("back", CameraSelector.LENS_FACING_BACK),
    Front("front", CameraSelector.LENS_FACING_FRONT),
}
