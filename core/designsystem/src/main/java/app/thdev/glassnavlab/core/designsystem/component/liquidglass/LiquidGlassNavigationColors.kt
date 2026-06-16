package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
internal fun rememberLiquidGlassResolvedColors(
    style: LiquidGlassNavigationStyle,
    adaptiveBackgroundColor: Color,
): LiquidGlassResolvedColors {
    val tone = rememberLiquidGlassTone(adaptiveBackgroundColor)
    val target = resolveLiquidGlassColors(style, tone)
    val colorAnimationSpec = tween<Color>(durationMillis = 650)
    val containerSurfaceColor by animateColorAsState(
        targetValue = target.containerSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid container surface color",
    )
    val selectedSurfaceColor by animateColorAsState(
        targetValue = target.selectedSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid selected surface color",
    )
    val actionSurfaceColor by animateColorAsState(
        targetValue = target.actionSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid action surface color",
    )
    val selectedContentColor by animateColorAsState(
        targetValue = target.selectedContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid selected content color",
    )
    val unselectedContentColor by animateColorAsState(
        targetValue = target.unselectedContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid unselected content color",
    )
    val actionContentColor by animateColorAsState(
        targetValue = target.actionContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid action content color",
    )

    return LiquidGlassResolvedColors(
        containerSurfaceColor = containerSurfaceColor,
        selectedSurfaceColor = selectedSurfaceColor,
        actionSurfaceColor = actionSurfaceColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        actionContentColor = actionContentColor,
    )
}

internal data class LiquidGlassResolvedColors(
    val containerSurfaceColor: Color,
    val selectedSurfaceColor: Color,
    val actionSurfaceColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val actionContentColor: Color,
)

@Composable
private fun rememberLiquidGlassTone(adaptiveBackgroundColor: Color): LiquidGlassTone? {
    if (adaptiveBackgroundColor == Color.Unspecified) return null

    var tone by remember {
        mutableStateOf(toneForLuminance(adaptiveBackgroundColor.luminance()))
    }

    LaunchedEffect(adaptiveBackgroundColor) {
        val luminance = adaptiveBackgroundColor.luminance()
        tone = when (tone) {
            LiquidGlassTone.Light -> {
                if (luminance < DarkToneLuminanceThreshold) {
                    LiquidGlassTone.Dark
                } else {
                    LiquidGlassTone.Light
                }
            }

            LiquidGlassTone.Dark -> {
                if (luminance > LightToneLuminanceThreshold) {
                    LiquidGlassTone.Light
                } else {
                    LiquidGlassTone.Dark
                }
            }
        }
    }

    return tone
}

private fun resolveLiquidGlassColors(
    style: LiquidGlassNavigationStyle,
    tone: LiquidGlassTone?,
): LiquidGlassResolvedColors {
    if (tone == null) {
        return LiquidGlassResolvedColors(
            containerSurfaceColor = style.containerSurfaceColor,
            selectedSurfaceColor = style.selectedSurfaceColor,
            actionSurfaceColor = style.selectedSurfaceColor,
            selectedContentColor = style.selectedContentColor,
            unselectedContentColor = style.unselectedContentColor,
            actionContentColor = style.selectedContentColor,
        )
    }

    return when (tone) {
        LiquidGlassTone.Light -> LiquidGlassResolvedColors(
            containerSurfaceColor = Color.White.copy(alpha = 0.13f),
            selectedSurfaceColor = Color(0xFFE1E5EA).copy(alpha = 0.64f),
            actionSurfaceColor = Color.White.copy(alpha = 0.17f),
            selectedContentColor = Color(0xFF111111),
            unselectedContentColor = Color(0xFF2F343A).copy(alpha = 0.52f),
            actionContentColor = Color(0xFF111111),
        )

        LiquidGlassTone.Dark -> LiquidGlassResolvedColors(
            containerSurfaceColor = Color(0xFF34414B).copy(alpha = 0.20f),
            selectedSurfaceColor = Color(0xFF7E8A94).copy(alpha = 0.38f),
            actionSurfaceColor = Color(0xFF65717B).copy(alpha = 0.24f),
            selectedContentColor = Color.White.copy(alpha = 0.98f),
            unselectedContentColor = Color(0xFFDCE6EF).copy(alpha = 0.54f),
            actionContentColor = Color.White.copy(alpha = 0.98f),
        )
    }
}

private fun toneForLuminance(luminance: Float): LiquidGlassTone {
    return if (luminance < InitialDarkToneLuminanceThreshold) {
        LiquidGlassTone.Dark
    } else {
        LiquidGlassTone.Light
    }
}

private enum class LiquidGlassTone {
    Light,
    Dark,
}

private const val InitialDarkToneLuminanceThreshold = 0.44f
private const val DarkToneLuminanceThreshold = 0.34f
private const val LightToneLuminanceThreshold = 0.58f
