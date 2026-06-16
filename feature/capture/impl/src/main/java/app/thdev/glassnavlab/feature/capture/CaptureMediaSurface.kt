package app.thdev.glassnavlab.feature.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidCaptureDraft
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidCaptureMediaState
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Composable
internal fun CaptureMediaSurface(
    destination: NotmidDestination,
    draft: NotmidCaptureDraft?,
    readyToPublish: Boolean,
    cameraState: CaptureCameraUiState,
    cameraController: CaptureCameraController,
    capturedMediaName: String?,
    onCameraModeChange: (CaptureCameraMode) -> Unit,
    onCameraLensChange: (CaptureCameraLens) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCameraStatusChange: (CaptureCameraStatus) -> Unit,
    onCapturePhoto: () -> Unit,
) {
    val palette = destination.clips.firstOrNull()?.palette.orEmpty()
    val startColor = palette.getOrNull(0) ?: NotmidColorTokens.Ink
    val midColor = palette.getOrNull(1) ?: NotmidColorTokens.WarmClip
    val endColor = palette.getOrNull(2) ?: NotmidColorTokens.RouteBlue
    val isCameraMode = cameraState.mode == CaptureCameraMode.Camera
    val isCameraPreviewVisible = isCameraMode && cameraState.permissionGranted
    val captureStatusLabel = when (cameraState.mode) {
        CaptureCameraMode.Camera -> if (capturedMediaName != null) {
            "photo captured"
        } else {
            cameraState.status.label
        }
        CaptureCameraMode.Upload -> "upload standby"
    }

    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(356.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassDark,
        borderColor = Color.White.copy(alpha = 0.28f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCameraPreviewVisible) {
                CaptureCameraPreview(
                    lens = cameraState.lens,
                    controller = cameraController,
                    onStatusChange = onCameraStatusChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            } else {
                CapturePreviewFallback(
                    startColor = startColor,
                    midColor = midColor,
                    endColor = endColor,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(NotmidTheme.spacing.xxl),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CaptureStatusPill(
                        label = if (readyToPublish) "draft ready" else "empty camera",
                        active = readyToPublish,
                    )
                    CaptureStatusPill(
                        label = captureStatusLabel,
                        active = capturedMediaName != null ||
                            cameraState.status == CaptureCameraStatus.Previewing,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
                    Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                        NotmidText(
                            text = if (isCameraMode) "Camera preview" else "Upload preview",
                            color = Color.White,
                            variant = NotmidTextVariant.Title,
                        )
                        NotmidText(
                            text = cameraState.supportingText(
                                draft = draft,
                                capturedMediaName = capturedMediaName,
                            ),
                            color = Color.White.copy(alpha = 0.78f),
                            variant = NotmidTextVariant.BodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                        CaptureCameraMode.entries.forEach { mode ->
                            NotmidPillButton(
                                label = mode.label,
                                selected = cameraState.mode == mode,
                                onClick = { onCameraModeChange(mode) },
                            )
                        }
                    }
                    if (isCameraMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                            CaptureCameraLens.entries.forEach { lens ->
                                NotmidPillButton(
                                    label = lens.label,
                                    selected = cameraState.lens == lens,
                                    enabled = cameraState.permissionGranted,
                                    onClick = { onCameraLensChange(lens) },
                                )
                            }
                        }
                    }
                    if (isCameraPreviewVisible) {
                        NotmidButton(
                            text = if (cameraController.isCapturing) {
                                "Capturing"
                            } else {
                                "Capture"
                            },
                            onClick = onCapturePhoto,
                            enabled = cameraState.status == CaptureCameraStatus.Previewing &&
                                !cameraController.isCapturing,
                            variant = NotmidButtonVariant.Secondary,
                        )
                    }
                    if (isCameraMode && !cameraState.permissionGranted) {
                        NotmidButton(
                            text = "Allow camera",
                            onClick = onRequestCameraPermission,
                            variant = NotmidButtonVariant.Secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturePreviewFallback(
    startColor: Color,
    midColor: Color,
    endColor: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(startColor, midColor, endColor),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * 0.38f,
            center = Offset(size.width * 0.78f, size.height * 0.18f),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.38f),
            start = Offset(size.width * 0.14f, size.height * 0.78f),
            end = Offset(size.width * 0.88f, size.height * 0.64f),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun CaptureCameraUiState.supportingText(
    draft: NotmidCaptureDraft?,
    capturedMediaName: String?,
): String {
    return when {
        capturedMediaName != null -> "Photo captured locally."
        mode == CaptureCameraMode.Upload -> "Upload picker is queued for the next capture slice."
        !permissionGranted -> "Camera permission is required before preview."
        status == CaptureCameraStatus.Failed -> "Preview could not start on this device."
        status == CaptureCameraStatus.Previewing -> "Preview stays local until publish."
        draft?.mediaState == NotmidCaptureMediaState.LocalPreview -> "Local draft media is ready."
        else -> "Preparing camera preview."
    }
}

@Composable
private fun CaptureStatusPill(
    label: String,
    active: Boolean,
) {
    val backgroundColor = if (active) {
        Color.White.copy(alpha = 0.26f)
    } else {
        Color.Black.copy(alpha = 0.28f)
    }
    NotmidGlassSurface(
        shape = NotmidTheme.shapes.pill,
        backgroundColor = backgroundColor,
        borderColor = Color.White.copy(alpha = 0.32f),
        contentPadding = PaddingValues(
            horizontal = NotmidTheme.spacing.md,
            vertical = NotmidTheme.spacing.sm,
        ),
    ) {
        NotmidText(
            text = label,
            color = Color.White,
            variant = NotmidTextVariant.Caption,
            maxLines = 1,
        )
    }
}
