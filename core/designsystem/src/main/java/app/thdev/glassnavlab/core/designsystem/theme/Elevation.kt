package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NotmidElevation(
    val none: Dp = 0.dp,
    val card: Dp = 2.dp,
    val floating: Dp = 12.dp,
    val sheet: Dp = 20.dp,
)

val DefaultNotmidElevation = NotmidElevation()
