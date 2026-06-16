package app.thdev.glassnavlab.feature.notmid.common.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGradientSummaryCard
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.component.notmidTextStyle
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidBadge
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip

@Composable
fun NotmidClipCard(
    clip: NotmidClip,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (clip.isLive) {
        NotmidLiveVideoCard(
            clip = clip,
            uiBadge = clip.badge,
            modifier = modifier,
            onClick = onClick,
        )
    } else {
        val labelText = when (val uiBadge = clip.badge) {
            is NotmidBadge.Label -> uiBadge.text
            NotmidBadge.LiveNow -> "LIVE"
            NotmidBadge.None -> ""
        }
        NotmidGradientSummaryCard(
            label = labelText,
            title = clip.title,
            description = clip.description,
            palette = clip.palette,
            modifier = modifier,
            onClick = onClick,
        )
    }
}

@Composable
private fun NotmidLiveVideoCard(
    clip: NotmidClip,
    uiBadge: NotmidBadge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = NotmidTheme.shapes.cardLarge

    // Flowing color physics simulation (60fps canvas movement)
    val infiniteTransition = rememberInfiniteTransition(label = "video_flow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "video_angle"
    )

    // Red dot pulsing animation
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Color(0xFF0F1216)),
        contentAlignment = Alignment.BottomStart,
    ) {
        // GPU Canvas Drawing representing the simulated video
        Canvas(modifier = Modifier.fillMaxSize()) {
            clipRect {
                val center = Offset(size.width / 2f, size.height / 2f)
                val rad = (angle * Math.PI / 180f).toFloat()

                // Overlapping blurred color blobs representing liquid/dynamic visual elements
                val blob1Center = Offset(
                    x = center.x + kotlin.math.cos(rad) * 140.dp.toPx(),
                    y = center.y + kotlin.math.sin(rad) * 80.dp.toPx()
                )
                val blob2Center = Offset(
                    x = center.x - kotlin.math.sin(rad * 1.2f) * 120.dp.toPx(),
                    y = center.y + kotlin.math.cos(rad * 0.9f) * 90.dp.toPx()
                )
                val blob3Center = Offset(
                    x = center.x + kotlin.math.cos(rad * 1.5f) * 90.dp.toPx(),
                    y = center.y - kotlin.math.sin(rad * 1.3f) * 100.dp.toPx()
                )

                // Personalize look dynamically using clip.palette if available
                val color1 = clip.palette.getOrNull(0) ?: Color(0xFFFF4D6D)
                val color2 = clip.palette.getOrNull(1) ?: Color(0xFFFFD166)
                val color3 = clip.palette.getOrNull(2) ?: Color(0xFF7209B7)

                // Render dynamic color circles with smooth radial gradients to simulate liquid video
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color1.copy(alpha = 0.85f), Color.Transparent),
                        center = blob1Center,
                        radius = 220.dp.toPx()
                    ),
                    center = blob1Center,
                    radius = 220.dp.toPx()
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color2.copy(alpha = 0.80f), Color.Transparent),
                        center = blob2Center,
                        radius = 200.dp.toPx()
                    ),
                    center = blob2Center,
                    radius = 200.dp.toPx()
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color3.copy(alpha = 0.75f), Color.Transparent),
                        center = blob3Center,
                        radius = 240.dp.toPx()
                    ),
                    center = blob3Center,
                    radius = 240.dp.toPx()
                )
            }
        }

        // Overlay with glassmorphism controls and text info
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NotmidTheme.spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: LIVE badge and HD indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Live Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = NotmidTheme.spacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE63946).copy(alpha = dotAlpha))
                    )

                    val labelText = when (uiBadge) {
                        is NotmidBadge.Label -> uiBadge.text
                        NotmidBadge.LiveNow -> "LIVE"
                        NotmidBadge.None -> ""
                    }
                    NotmidText(
                        text = labelText.uppercase(),
                        color = Color.White,
                        variant = NotmidTextVariant.Caption,
                        style = NotmidTextVariant.Caption.notmidTextStyle().copy(fontWeight = FontWeight.Bold),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    NotmidText(
                        text = clip.qualityLabel,
                        color = Color.White,
                        variant = NotmidTextVariant.Caption,
                        style = NotmidTextVariant.Caption.notmidTextStyle().copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                    )
                }
            }

            // Bottom Column: Text details and Player Controls overlay
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
            ) {
                // Glassmorphism Text background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(NotmidTheme.spacing.md)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        NotmidText(
                            text = clip.title,
                            color = Color.White,
                            variant = NotmidTextVariant.Headline,
                            style = NotmidTextVariant.Headline.notmidTextStyle().copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        NotmidText(
                            text = clip.description,
                            color = Color.White.copy(alpha = 0.85f),
                            variant = NotmidTextVariant.Label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Player Controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = NotmidTheme.spacing.md, vertical = NotmidTheme.spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Play Triangle (drawn via Canvas)
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        val progress = clip.playbackProgress.coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(Color(0xFFFF4D6D))
                        )
                    }

                    // Volume Icon speaker shape (drawn via Canvas)
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val speaker = Path().apply {
                            moveTo(0f, size.height * 0.35f)
                            lineTo(size.width * 0.4f, size.height * 0.35f)
                            lineTo(size.width * 0.7f, size.height * 0.1f)
                            lineTo(size.width * 0.7f, size.height * 0.9f)
                            lineTo(size.width * 0.4f, size.height * 0.65f)
                            lineTo(0f, size.height * 0.65f)
                            close()
                        }
                        drawPath(speaker, Color.White)

                        // Volume wave line (re-centered inside bounding box to avoid right edge clipping)
                        drawArc(
                            color = Color.White,
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
                            size = Size(size.width * 0.8f, size.height * 0.7f),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
