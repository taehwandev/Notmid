package app.thdev.glassnavlab.feature.capture

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File

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
