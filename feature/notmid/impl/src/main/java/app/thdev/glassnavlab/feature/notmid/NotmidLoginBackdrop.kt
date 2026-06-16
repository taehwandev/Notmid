package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
internal fun KineticLoginBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(LoginBackground, LoginSurfaceContainer),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawLine(
            color = LoginPrimaryContainer.copy(alpha = 0.22f),
            start = Offset(-size.width * 0.24f, size.height * 0.16f),
            end = Offset(size.width * 1.08f, size.height * 0.04f),
            strokeWidth = 58f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = LoginOnSurface.copy(alpha = 0.08f),
            start = Offset(-size.width * 0.16f, size.height * 0.34f),
            end = Offset(size.width * 1.18f, size.height * 0.24f),
            strokeWidth = 106f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = LoginTertiaryFixed.copy(alpha = 0.14f),
            start = Offset(-size.width * 0.1f, size.height * 0.62f),
            end = Offset(size.width * 1.08f, size.height * 0.44f),
            strokeWidth = 64f,
            cap = StrokeCap.Round,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, LoginBackground),
                startY = size.height * 0.36f,
                endY = size.height,
            ),
        )
    }
}
