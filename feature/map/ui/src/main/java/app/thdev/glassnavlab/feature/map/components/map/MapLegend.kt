package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme


@Composable
internal fun MapLegend(
    pinCount: Int,
    modifier: Modifier = Modifier,
) {
    NotmidGlassSurface(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        backgroundColor = Color.White.copy(alpha = 0.64f),
        contentPadding = PaddingValues(
            horizontal = NotmidTheme.spacing.md,
            vertical = NotmidTheme.spacing.sm,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
            NotmidText(
                text = "Seoul fake map",
                variant = NotmidTextVariant.Label,
                maxLines = 1,
            )
            NotmidText(
                text = "$pinCount receipts visible",
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.Caption,
                maxLines = 1,
            )
        }
    }
}
