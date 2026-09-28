package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.feature.map.components.map.FakeMapCanvas
import app.thdev.glassnavlab.feature.map.components.map.MapBoardSurface
import app.thdev.glassnavlab.feature.map.components.places.PlacePreviewSheet
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
internal fun MapContent(state: MapUiState.Ready, onAction: (MapAction) -> Unit) {
    val selectedVisiblePin = state.selectedPin?.takeIf { selected ->
        state.visiblePins.any { it.place.id == selected.place.id }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FakeMapCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = NotmidTheme.spacing.screenTop,
                    bottom = NotmidTheme.spacing.bottomNavigationPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
        ) {
            MapHeading(
                modifier = Modifier.padding(horizontal = NotmidTheme.spacing.screenHorizontal),
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = NotmidTheme.spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
            ) {
                items(state.categories, key = { it }) { category ->
                    NotmidPillButton(
                        label = category,
                        selected = category == state.selectedCategory,
                        onClick = { onAction(MapAction.CategorySelected(category)) },
                    )
                }
            }

            MapBoardSurface(
                pins = state.visiblePins,
                selectedPlaceId = selectedVisiblePin?.place?.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onPinSelected = { pin -> onAction(MapAction.PinSelected(pin.place.id)) },
            )

            PlacePreviewSheet(
                pin = selectedVisiblePin,
                modifier = Modifier.padding(horizontal = NotmidTheme.spacing.screenHorizontal),
                onOpenPlace = { onAction(MapAction.OpenSelectedPlace) },
            )
        }
    }
}

@Composable
private fun MapHeading(modifier: Modifier = Modifier) {
    NotmidGlassSurface(
        modifier = modifier,
        shape = NotmidTheme.shapes.card,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(
            horizontal = NotmidTheme.spacing.md,
            vertical = NotmidTheme.spacing.sm,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
            NotmidText(
                text = stringResource(R.string.map_heading),
                variant = NotmidTextVariant.Headline,
            )
            NotmidText(
                text = stringResource(R.string.map_heading_supporting),
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.Caption,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MapEmptyPreview() {
    notmidTheme {
        MapContent(
            MapUiState.Ready("map", "Map", DefaultMapCategories, "All", emptyList(), null, emptyList()),
            {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MapSelectedPlacePreview() {
    val cafe = MapPinUi(
        place = NotmidPlace(
            id = "preview-cafe",
            title = "Corner Cafe",
            description = "Quiet tables and fresh receipts today",
            metric = "4.8",
            palette = emptyList(),
            height = 176.dp,
            contentColor = NotmidColorTokens.Ink,
            category = "Cafe",
        ),
        category = "Cafe",
        xFraction = 0.28f,
        yFraction = 0.36f,
    )
    notmidTheme {
        MapContent(
            MapUiState.Ready("map", "Map", DefaultMapCategories, "All", listOf(cafe), cafe, emptyList()),
            {},
        )
    }
}
