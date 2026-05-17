package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidGradientHeroCard(
    title: String,
    description: String,
    metric: String,
    palette: List<Color>,
    height: Dp,
    modifier: Modifier = Modifier,
    contentColor: Color = NotmidTheme.colors.contentOnMedia,
    emphasizedTitle: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = NotmidTheme.shapes.sheet
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Brush.linearGradient(palette.ifEmpty { DefaultHeroPalette }))
            .padding(NotmidTheme.spacing.xxl),
    ) {
        NotmidText(
            text = metric,
            modifier = Modifier.align(Alignment.TopEnd),
            color = contentColor.copy(alpha = 0.72f),
            variant = NotmidTextVariant.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
        ) {
            NotmidText(
                text = title,
                color = contentColor,
                style = if (emphasizedTitle) {
                    NotmidTheme.typography.title.copy(fontSize = 25.sp)
                } else {
                    NotmidTheme.typography.title
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            NotmidText(
                text = description,
                color = contentColor.copy(alpha = 0.76f),
                variant = NotmidTextVariant.BodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val DefaultHeroPalette = listOf(
    Color(0xFF101114),
    Color(0xFF2D333B),
    Color(0xFF6B7178),
)
