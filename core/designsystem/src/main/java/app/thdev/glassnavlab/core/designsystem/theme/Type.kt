package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val NotmidFontFamily = FontFamily.Default

@Immutable
data class NotmidTypography(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

val DefaultNotmidTypography = NotmidTypography(
    display = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    title = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    headline = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    body = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    label = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    caption = TextStyle(
        fontFamily = NotmidFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)

val MaterialNotmidTypography = Typography(
    displayMedium = DefaultNotmidTypography.display,
    titleLarge = DefaultNotmidTypography.title,
    titleMedium = DefaultNotmidTypography.headline,
    bodyLarge = DefaultNotmidTypography.body,
    bodyMedium = DefaultNotmidTypography.bodySmall,
    labelLarge = DefaultNotmidTypography.label,
    labelSmall = DefaultNotmidTypography.caption,
)
