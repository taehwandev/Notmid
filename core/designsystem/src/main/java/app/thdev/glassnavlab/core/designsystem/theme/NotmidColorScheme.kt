package app.thdev.glassnavlab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
