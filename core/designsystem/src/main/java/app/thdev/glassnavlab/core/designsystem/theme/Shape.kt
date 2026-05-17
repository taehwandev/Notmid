package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class NotmidShapes(
    val control: Shape = RoundedCornerShape(8.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val cardLarge: Shape = RoundedCornerShape(24.dp),
    val sheet: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(999.dp),
)

val DefaultNotmidShapes = NotmidShapes()

val MaterialNotmidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
