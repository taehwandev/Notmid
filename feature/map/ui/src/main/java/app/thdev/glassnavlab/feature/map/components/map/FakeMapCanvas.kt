package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens


@Composable
internal fun FakeMapCanvas(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE8ECEA),
                    Color(0xFFF8F0DF),
                    Color(0xFFDCE8F6),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )

        val streetStroke = Stroke(
            width = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.05f, size.height * 0.22f)
                cubicTo(
                    size.width * 0.28f,
                    size.height * 0.12f,
                    size.width * 0.44f,
                    size.height * 0.34f,
                    size.width * 0.95f,
                    size.height * 0.20f,
                )
            },
            color = Color.White.copy(alpha = 0.82f),
            style = streetStroke,
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.88f)
                cubicTo(
                    size.width * 0.34f,
                    size.height * 0.62f,
                    size.width * 0.58f,
                    size.height * 0.58f,
                    size.width * 0.84f,
                    size.height * 0.10f,
                )
            },
            color = Color.White.copy(alpha = 0.74f),
            style = streetStroke,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.64f),
            start = Offset(size.width * 0.08f, size.height * 0.58f),
            end = Offset(size.width * 0.92f, size.height * 0.72f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = NotmidColorTokens.RouteBlue.copy(alpha = 0.12f),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.22f, size.height * 0.30f),
        )
        drawCircle(
            color = NotmidColorTokens.SignalGreen.copy(alpha = 0.13f),
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.76f, size.height * 0.74f),
        )
    }
}
