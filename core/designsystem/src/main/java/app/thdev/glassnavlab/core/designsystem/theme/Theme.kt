package app.thdev.glassnavlab.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalNotmidColors = staticCompositionLocalOf { NotmidLightColorScheme }
private val LocalNotmidTypography = staticCompositionLocalOf { DefaultNotmidTypography }
private val LocalNotmidSpacing = staticCompositionLocalOf { DefaultNotmidSpacing }
private val LocalNotmidShapes = staticCompositionLocalOf { DefaultNotmidShapes }
private val LocalNotmidElevation = staticCompositionLocalOf { DefaultNotmidElevation }

private val NotmidMaterialLightColorScheme = lightColorScheme(
    primary = NotmidColorTokens.Ink,
    onPrimary = NotmidColorTokens.Cloud,
    secondary = NotmidColorTokens.RouteBlue,
    onSecondary = NotmidColorTokens.Cloud,
    tertiary = NotmidColorTokens.WarmClip,
    onTertiary = NotmidColorTokens.Ink,
    background = NotmidLightColorScheme.background,
    onBackground = NotmidLightColorScheme.content,
    surface = NotmidLightColorScheme.surface,
    onSurface = NotmidLightColorScheme.content,
    surfaceVariant = NotmidLightColorScheme.surfaceRaised,
    onSurfaceVariant = NotmidLightColorScheme.contentMuted,
    outline = NotmidLightColorScheme.line,
    error = NotmidLightColorScheme.danger,
)

private val NotmidMaterialDarkColorScheme = darkColorScheme(
    primary = NotmidColorTokens.Cloud,
    onPrimary = NotmidColorTokens.Ink,
    secondary = NotmidColorTokens.RouteBlue,
    onSecondary = NotmidColorTokens.Cloud,
    tertiary = NotmidColorTokens.WarmClip,
    onTertiary = NotmidColorTokens.Ink,
    background = NotmidDarkColorScheme.background,
    onBackground = NotmidDarkColorScheme.content,
    surface = NotmidDarkColorScheme.surface,
    onSurface = NotmidDarkColorScheme.content,
    surfaceVariant = NotmidDarkColorScheme.surfaceRaised,
    onSurfaceVariant = NotmidDarkColorScheme.contentMuted,
    outline = NotmidDarkColorScheme.line,
    error = NotmidDarkColorScheme.danger,
)

object NotmidTheme {
    val colors: NotmidColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalNotmidColors.current

    val typography: NotmidTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNotmidTypography.current

    val spacing: NotmidSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalNotmidSpacing.current

    val shapes: NotmidShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNotmidShapes.current

    val elevation: NotmidElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalNotmidElevation.current
}

@Composable
fun notmidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val notmidColors = if (darkTheme) NotmidDarkColorScheme else NotmidLightColorScheme
    val materialColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Activity -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> NotmidMaterialDarkColorScheme
        else -> NotmidMaterialLightColorScheme
    }

    CompositionLocalProvider(
        LocalNotmidColors provides notmidColors,
        LocalNotmidTypography provides DefaultNotmidTypography,
        LocalNotmidSpacing provides DefaultNotmidSpacing,
        LocalNotmidShapes provides DefaultNotmidShapes,
        LocalNotmidElevation provides DefaultNotmidElevation,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = MaterialNotmidTypography,
            shapes = MaterialNotmidShapes,
            content = content,
        )
    }
}
