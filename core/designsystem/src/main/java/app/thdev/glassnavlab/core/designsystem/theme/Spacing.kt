package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NotmidSpacing(
    val xxs: Dp = 3.dp,
    val xs: Dp = 6.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenTop: Dp = 64.dp,
    val bottomNavigationPadding: Dp = 150.dp,
    val bottomNavigationSampleOffset: Dp = 88.dp,
)

val DefaultNotmidSpacing = NotmidSpacing()
