package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun FeedPlaceStrip(
    place: FeedPlaceUi?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = NotmidTheme.shapes.cardLarge,
        backgroundColor = NotmidTheme.colors.glassDark,
        borderColor = Color.White.copy(alpha = 0.28f),
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedPlaceSwatch(place = place)
            FeedPlaceCopy(
                place = place,
                modifier = Modifier.weight(1f),
            )
            NotmidButton(
                text = "Open",
                onClick = onClick,
                variant = NotmidButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun FeedPlaceCopy(
    place: FeedPlaceUi?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
    ) {
        NotmidText(
            text = place?.title ?: "Linked place",
            color = Color.White,
            variant = NotmidTextVariant.Headline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NotmidText(
            text = place?.subtitle ?: "Open the receipt to inspect the attached place.",
            color = Color.White.copy(alpha = 0.72f),
            variant = NotmidTextVariant.Caption,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeedPlaceSwatch(
    place: FeedPlaceUi?,
) {
    val colors = place?.palette?.takeIf(List<Color>::isNotEmpty)
        ?: listOf(NotmidColorTokens.RouteBlue, NotmidColorTokens.SignalGreen)
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(NotmidTheme.shapes.card)
            .background(Brush.linearGradient(colors)),
    )
}
