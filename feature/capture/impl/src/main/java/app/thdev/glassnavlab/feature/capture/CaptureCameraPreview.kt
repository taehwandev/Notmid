package app.thdev.glassnavlab.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

internal data class CaptureCameraUiState(
    val mode: CaptureCameraMode,
    val lens: CaptureCameraLens,
    val permissionGranted: Boolean,
    val status: CaptureCameraStatus,
)

internal enum class CaptureCameraMode(
    val label: String,
) {
    Camera("camera"),
    Upload("upload"),
}

internal enum class CaptureCameraLens(
    val label: String,
    val lensFacing: Int,
) {
    Back("back", CameraSelector.LENS_FACING_BACK),
    Front("front", CameraSelector.LENS_FACING_FRONT),
}

internal enum class CaptureCameraStatus(
    val label: String,
) {
    PermissionRequired("camera blocked"),
    Starting("starting camera"),
    Previewing("live preview"),
    Failed("camera unavailable"),
}

internal data class CaptureCameraMedia(
    val uriString: String,
    val fileName: String,
)

@Stable
internal class CaptureCameraController {
    var isCapturing by mutableStateOf(false)
        private set

    private var imageCapture: ImageCapture? = null

    internal fun bind(imageCapture: ImageCapture) {
        this.imageCapture = imageCapture
    }

    internal fun clear(imageCapture: ImageCapture?) {
        if (this.imageCapture === imageCapture) {
            this.imageCapture = null
        }
    }

    fun captureStill(
        context: Context,
        onCaptured: (CaptureCameraMedia) -> Unit,
        onError: () -> Unit,
    ) {
        val activeImageCapture = imageCapture
        if (activeImageCapture == null) {
            onError()
            return
        }

        val outputFile = File(
            context.cacheDir,
            "notmid-capture-${System.currentTimeMillis()}.jpg",
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        val mainExecutor = ContextCompat.getMainExecutor(context)

        isCapturing = true
        activeImageCapture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onCaptured(
                        CaptureCameraMedia(
                            uriString = outputFileResults.savedUri?.toString()
                                ?: outputFile.toUri().toString(),
                            fileName = outputFile.name,
                        ),
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    onError()
                }
            },
        )
    }
}

@Composable
internal fun rememberCaptureCameraController(): CaptureCameraController {
    return remember { CaptureCameraController() }
}

@Composable
internal fun CaptureCameraPreview(
    lens: CaptureCameraLens,
    controller: CaptureCameraController,
    onStatusChange: (CaptureCameraStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStatusChange by rememberUpdatedState(onStatusChange)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(context, lifecycleOwner, lens) {
        var isDisposed = false
        var boundImageCapture: ImageCapture? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        currentOnStatusChange(CaptureCameraStatus.Starting)
        cameraProviderFuture.addListener(
            Runnable {
                if (isDisposed) return@Runnable
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lens.lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                    )
                    boundImageCapture = imageCapture
                    controller.bind(imageCapture)
                }.onSuccess {
                    currentOnStatusChange(CaptureCameraStatus.Previewing)
                }.onFailure {
                    currentOnStatusChange(CaptureCameraStatus.Failed)
                }
            },
            mainExecutor,
        )

        onDispose {
            isDisposed = true
            controller.clear(boundImageCapture)
            if (cameraProviderFuture.isDone) {
                runCatching {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

internal fun Context.isCaptureCameraPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}
