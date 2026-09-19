package app.thdev.glassnavlab.feature.capture

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

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
