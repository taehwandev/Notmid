package app.thdev.glassnavlab.feature.map.components.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.map.R

@Composable
internal fun MapLegend(
    hasPins: Boolean,
    modifier: Modifier = Modifier,
) {
    NotmidGlassSurface(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(
            horizontal = NotmidTheme.spacing.md,
            vertical = NotmidTheme.spacing.sm,
        ),
    ) {
        NotmidText(
            text = stringResource(if (hasPins) R.string.map_places_in_view else R.string.map_no_places_in_view),
            variant = NotmidTextVariant.Caption,
            maxLines = 1,
        )
    }
}
