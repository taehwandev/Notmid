package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

import app.thdev.glassnavlab.feature.map.components.map.MapBoardSurface
import app.thdev.glassnavlab.feature.map.components.places.PlacePreviewSheet

@Composable
internal fun MapContent(state: MapUiState.Ready, listState: LazyListState, onAction: (MapAction) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NotmidColorTokens.WarmMist),
        state = listState,
        contentPadding = PaddingValues(
            start = NotmidTheme.spacing.screenHorizontal,
            top = NotmidTheme.spacing.screenTop,
            end = NotmidTheme.spacing.screenHorizontal,
            bottom = NotmidTheme.spacing.bottomNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "map-header-${state.destinationId}") {
            NotmidSectionHeader(
                title = "proof map",
                subtitle = "Fake local pins show where fresh receipts are active right now.",
                eyebrow = state.title,
            )
        }

        item(key = "map-categories-${state.destinationId}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                items(state.categories, key = { it }) { category ->
                    NotmidPillButton(
                        label = category,
                        selected = category == state.selectedCategory,
                        onClick = {
                            onAction(MapAction.CategorySelected(category))
                        },
                    )
                }
            }
        }

        item(key = "map-board-${state.destinationId}-${state.selectedCategory}") {
            MapBoardSurface(
                pins = state.visiblePins,
                selectedPlaceId = state.selectedPin?.place?.id,
                onPinSelected = { pin -> onAction(MapAction.PinSelected(pin.place.id)) },
            )
        }

        item(key = "map-preview-${state.destinationId}-${state.selectedPin?.place?.id}") {
            PlacePreviewSheet(
                pin = state.selectedPin,
                visiblePinCount = state.visiblePins.size,
                onOpenPlace = {
                    onAction(MapAction.OpenSelectedPlace)
                },
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
            rememberLazyListState(), {},
        )
    }
}
