package app.thdev.glassnavlab.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextField
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
internal fun CapturePlacePicker(
    place: NotmidPlace?,
    places: List<NotmidPlace>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (String) -> Unit,
) {
    val matches = places.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.address.contains(query, ignoreCase = true)
    }
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            NotmidText(text = "Attach a place", variant = NotmidTextVariant.Headline)
            NotmidTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = "Find a place",
                placeholder = "Name or neighborhood",
                singleLine = true,
            )
            if (places.isEmpty()) {
                NotmidText(
                    text = "No places are available yet. Try again when nearby places load.",
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.BodySmall,
                )
            } else if (matches.isEmpty()) {
                NotmidText(
                    text = "No matching place. Try another name or neighborhood.",
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.BodySmall,
                )
            } else {
                matches.take(5).forEach { option ->
                    NotmidPillButton(
                        label = option.title,
                        selected = option.id == place?.id,
                        onClick = { onPlaceSelected(option.id) },
                    )
                }
            }
        }
    }
    CapturePlaceAttachment(place)
}
