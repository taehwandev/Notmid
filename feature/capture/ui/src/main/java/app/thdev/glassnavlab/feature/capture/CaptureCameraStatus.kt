package app.thdev.glassnavlab.feature.capture

internal enum class CaptureCameraStatus(
    val label: String,
) {
    PermissionRequired("camera blocked"),
    Starting("starting camera"),
    Previewing("live preview"),
    Failed("camera unavailable"),
}
