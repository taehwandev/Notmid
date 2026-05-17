package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
fun NotmidMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    NotmidGlassSurface(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        backgroundColor = NotmidTheme.colors.glassLight,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
            NotmidText(
                text = label,
                color = NotmidTheme.colors.contentSubtle,
                variant = NotmidTextVariant.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            NotmidText(
                text = value,
                color = NotmidTheme.colors.content,
                variant = NotmidTextVariant.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
