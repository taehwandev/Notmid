package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object NotmidColorTokens {
    val Ink = Color(0xFF101114)
    val Slate = Color(0xFF2D333B)
    val Muted = Color(0xFF575D64)
    val Subtle = Color(0xFF6B7178)
    val Mist = Color(0xFFF4F6F8)
    val WarmMist = Color(0xFFF3F1EC)
    val Cloud = Color(0xFFFFFFFF)
    val Line = Color(0x1A101114)

    val SignalGreen = Color(0xFF14B87A)
    val RouteBlue = Color(0xFF2F6BFF)
    val WarmClip = Color(0xFFFF704D)
    val NightViolet = Color(0xFF7A4EF3)
    val AlertRed = Color(0xFFF04452)

    val LightGlass = Color(0xADFFFFFF)
    val LightGlassStrong = Color(0xD9FFFFFF)
    val DarkGlass = Color(0x8F121418)
    val GlassStroke = Color(0x59FFFFFF)
    val Shadow = Color(0x2E000000)
}

@Immutable
data class NotmidColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceInverse: Color,
    val content: Color,
    val contentMuted: Color,
    val contentSubtle: Color,
    val contentOnMedia: Color,
    val line: Color,
    val glassLight: Color,
    val glassLightStrong: Color,
    val glassDark: Color,
    val glassStroke: Color,
    val signal: Color,
    val route: Color,
    val clip: Color,
    val night: Color,
    val danger: Color,
)

val NotmidLightColorScheme = NotmidColorScheme(
    background = NotmidColorTokens.WarmMist,
    surface = NotmidColorTokens.Mist,
    surfaceRaised = NotmidColorTokens.Cloud,
    surfaceInverse = NotmidColorTokens.Ink,
    content = NotmidColorTokens.Ink,
    contentMuted = NotmidColorTokens.Muted,
    contentSubtle = NotmidColorTokens.Subtle,
    contentOnMedia = NotmidColorTokens.Cloud,
    line = NotmidColorTokens.Line,
    glassLight = NotmidColorTokens.LightGlass,
    glassLightStrong = NotmidColorTokens.LightGlassStrong,
    glassDark = NotmidColorTokens.DarkGlass,
    glassStroke = NotmidColorTokens.GlassStroke,
    signal = NotmidColorTokens.SignalGreen,
    route = NotmidColorTokens.RouteBlue,
    clip = NotmidColorTokens.WarmClip,
    night = NotmidColorTokens.NightViolet,
    danger = NotmidColorTokens.AlertRed,
)

val NotmidDarkColorScheme = NotmidLightColorScheme.copy(
    background = Color(0xFF0E1013),
    surface = Color(0xFF171A1E),
    surfaceRaised = Color(0xFF21262C),
    surfaceInverse = NotmidColorTokens.Cloud,
    content = Color(0xFFF7F8FA),
    contentMuted = Color(0xFFC1C7CE),
    contentSubtle = Color(0xFF89919B),
    line = Color(0x29FFFFFF),
    glassLight = Color(0x24FFFFFF),
    glassLightStrong = Color(0x36FFFFFF),
)
