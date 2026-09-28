package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun FakeMapCanvas(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(color = Color(0xFFE7ECE8))
        drawRect(
            color = Color(0xFFD8E6D8),
            topLeft = Offset(size.width * 0.04f, size.height * 0.04f),
            size = Size(size.width * 0.36f, size.height * 0.27f),
        )
        drawRect(
            color = Color(0xFFF0E6D9),
            topLeft = Offset(size.width * 0.65f, size.height * 0.54f),
            size = Size(size.width * 0.28f, size.height * 0.30f),
        )

        for (fraction in listOf(0.11f, 0.31f, 0.52f, 0.73f, 0.92f)) {
            drawLine(
                color = Color.White.copy(alpha = 0.72f),
                start = Offset(size.width * fraction, 0f),
                end = Offset(size.width * fraction, size.height),
                strokeWidth = 3.dp.toPx(),
            )
        }
        for (fraction in listOf(0.10f, 0.29f, 0.47f, 0.66f, 0.85f)) {
            drawLine(
                color = Color.White.copy(alpha = 0.72f),
                start = Offset(0f, size.height * fraction),
                end = Offset(size.width, size.height * fraction),
                strokeWidth = 3.dp.toPx(),
            )
        }

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
    }
}
