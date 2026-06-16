package app.thdev.glassnavlab.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidMetricTile
import app.thdev.glassnavlab.core.designsystem.component.NotmidOutlinedButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.map.api.MapRouteEvent
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidGeoPoint
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

private val DefaultMapCategories = listOf("All", "Cafe", "Work", "Night", "Exhibit", "Walk")

private data class MapPinUi(
    val place: NotmidPlace,
    val category: String,
    val xFraction: Float,
    val yFraction: Float,
)

@Composable
fun MapScreen(
    destination: NotmidDestination,
    listState: LazyListState,
    onRouteEvent: (MapRouteEvent) -> Unit = {},
) {
    val pins = remember(destination.id, destination.places) {
        destination.places.mapIndexed { index, place -> place.toMapPin(index) }
    }
    val categories = remember(pins) {
        listOf("All") + pins
            .map { it.category }
            .filter(String::isNotBlank)
            .distinct()
            .ifEmpty { DefaultMapCategories.drop(1) }
    }
    var selectedCategory by rememberSaveable(destination.id) { mutableStateOf("All") }
    var selectedPlaceId by rememberSaveable(destination.id) {
        mutableStateOf(destination.places.firstOrNull()?.id.orEmpty())
    }
    val visiblePins = remember(pins, selectedCategory) {
        if (selectedCategory == "All") pins else pins.filter { it.category == selectedCategory }
    }
    val selectedPin = visiblePins.firstOrNull { it.place.id == selectedPlaceId }
        ?: visiblePins.firstOrNull()
        ?: pins.firstOrNull()

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
        item(key = "map-header-${destination.id}") {
            NotmidSectionHeader(
                title = "proof map",
                subtitle = "Fake local pins show where fresh receipts are active right now.",
                eyebrow = destination.title,
            )
        }

        item(key = "map-categories-${destination.id}") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                items(categories, key = { it }) { category ->
                    NotmidPillButton(
                        label = category,
                        selected = category == selectedCategory,
                        onClick = {
                            selectedCategory = category
                            selectedPlaceId = pins.firstOrNull {
                                category == "All" || it.category == category
                            }?.place?.id.orEmpty()
                        },
                    )
                }
            }
        }

        item(key = "map-board-${destination.id}-$selectedCategory") {
            MapBoardSurface(
                pins = visiblePins,
                selectedPlaceId = selectedPin?.place?.id,
                onPinSelected = { pin -> selectedPlaceId = pin.place.id },
            )
        }

        item(key = "map-preview-${destination.id}-${selectedPin?.place?.id}") {
            PlacePreviewSheet(
                pin = selectedPin,
                visiblePinCount = visiblePins.size,
                onOpenPlace = {
                    selectedPin?.place?.id?.let { placeId ->
                        onRouteEvent(MapRouteEvent.PlaceRequested(placeId))
                    }
                },
            )
        }
    }
}

@Composable
private fun MapBoardSurface(
    pins: List<MapPinUi>,
    selectedPlaceId: String?,
    onPinSelected: (MapPinUi) -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            FakeMapCanvas(
                modifier = Modifier.fillMaxSize(),
            )
            NotmidText(
                text = "${pins.size} live pins",
                color = NotmidTheme.colors.content.copy(alpha = 0.22f),
                variant = NotmidTextVariant.Title,
                modifier = Modifier.align(Alignment.Center),
            )
            pins.forEachIndexed { index, pin ->
                val selected = pin.place.id == selectedPlaceId
                MapPin(
                    pin = pin,
                    selected = selected,
                    index = index,
                    modifier = Modifier.offset(
                        x = pinOffset(maxWidth, pin.xFraction),
                        y = pinOffset(maxHeight, pin.yFraction),
                    ),
                    onClick = { onPinSelected(pin) },
                )
            }

            MapLegend(
                pinCount = pins.size,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(NotmidTheme.spacing.lg),
            )
        }
    }
}

@Composable
private fun FakeMapCanvas(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE8ECEA),
                    Color(0xFFF8F0DF),
                    Color(0xFFDCE8F6),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )

        val streetStroke = Stroke(
            width = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.05f, size.height * 0.22f)
                cubicTo(
                    size.width * 0.28f,
                    size.height * 0.12f,
                    size.width * 0.44f,
                    size.height * 0.34f,
                    size.width * 0.95f,
                    size.height * 0.20f,
                )
            },
            color = Color.White.copy(alpha = 0.82f),
            style = streetStroke,
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.88f)
                cubicTo(
                    size.width * 0.34f,
                    size.height * 0.62f,
                    size.width * 0.58f,
                    size.height * 0.58f,
                    size.width * 0.84f,
                    size.height * 0.10f,
                )
            },
            color = Color.White.copy(alpha = 0.74f),
            style = streetStroke,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.64f),
            start = Offset(size.width * 0.08f, size.height * 0.58f),
            end = Offset(size.width * 0.92f, size.height * 0.72f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = NotmidColorTokens.RouteBlue.copy(alpha = 0.12f),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.22f, size.height * 0.30f),
        )
        drawCircle(
            color = NotmidColorTokens.SignalGreen.copy(alpha = 0.13f),
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.76f, size.height * 0.74f),
        )
    }
}

@Composable
private fun MapPin(
    pin: MapPinUi,
    selected: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val pinSize = if (selected) 76.dp else 62.dp
    val palette = pin.place.palette.ifEmpty {
        listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
    }
    Box(
        modifier = modifier
            .size(pinSize)
            .clip(NotmidTheme.shapes.pill)
            .background(
                brush = Brush.linearGradient(palette),
                shape = NotmidTheme.shapes.pill,
            )
            .clickable(onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        NotmidGlassSurface(
            shape = NotmidTheme.shapes.pill,
            backgroundColor = if (selected) {
                Color.White.copy(alpha = 0.44f)
            } else {
                Color.Black.copy(alpha = 0.22f)
            },
            borderColor = Color.White.copy(alpha = 0.34f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NotmidText(
                    text = "${index + 1}",
                    color = Color.White,
                    variant = NotmidTextVariant.Label,
                    maxLines = 1,
                )
                if (selected) {
                    NotmidText(
                        text = pin.category,
                        color = Color.White.copy(alpha = 0.86f),
                        variant = NotmidTextVariant.Caption,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLegend(
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

@Composable
private fun PlacePreviewSheet(
    pin: MapPinUi?,
    visiblePinCount: Int,
    onOpenPlace: () -> Unit,
) {
    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        if (pin == null) {
            NotmidText(
                text = "No pins match this filter.",
                color = NotmidTheme.colors.contentMuted,
            )
            return@NotmidGlassSurface
        }

        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            brush = Brush.linearGradient(
                                pin.place.palette.ifEmpty {
                                    listOf(NotmidColorTokens.Ink, NotmidColorTokens.RouteBlue)
                                },
                            ),
                            shape = NotmidTheme.shapes.card,
                        ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs),
                ) {
                    NotmidText(
                        text = pin.place.title,
                        variant = NotmidTextVariant.Headline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = pin.place.description,
                        color = NotmidTheme.colors.contentMuted,
                        variant = NotmidTextVariant.BodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidMetricTile(
                    label = "score",
                    value = pin.place.metric,
                    modifier = Modifier.weight(1f),
                )
                NotmidMetricTile(
                    label = "category",
                    value = pin.category,
                    modifier = Modifier.weight(1f),
                )
                NotmidMetricTile(
                    label = "nearby",
                    value = "$visiblePinCount",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm)) {
                NotmidOutlinedButton(
                    text = "Save later",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                NotmidButton(
                    text = "Open place",
                    onClick = onOpenPlace,
                    modifier = Modifier.weight(1f),
                    variant = NotmidButtonVariant.Secondary,
                )
            }
        }
    }
}

private fun NotmidPlace.toMapPin(index: Int): MapPinUi {
    val coordinates = coordinate?.toMapFractions()
        ?: MapPinCoordinates[index % MapPinCoordinates.size]
    return MapPinUi(
        place = this,
        category = category.ifBlank {
            DefaultMapCategories[(index % (DefaultMapCategories.size - 1)) + 1]
        },
        xFraction = coordinates.first,
        yFraction = coordinates.second,
    )
}

private fun pinOffset(
    axis: Dp,
    fraction: Float,
): Dp {
    return (axis * fraction) - 31.dp
}

private val MapPinCoordinates = listOf(
    0.26f to 0.24f,
    0.62f to 0.34f,
    0.42f to 0.64f,
    0.78f to 0.70f,
    0.18f to 0.76f,
    0.74f to 0.18f,
)

private fun NotmidGeoPoint.toMapFractions(): Pair<Float, Float> {
    val minLatitude = 37.50
    val maxLatitude = 37.59
    val minLongitude = 126.88
    val maxLongitude = 127.08
    val x = ((longitude - minLongitude) / (maxLongitude - minLongitude)).toFloat()
    val y = (1f - ((latitude - minLatitude) / (maxLatitude - minLatitude)).toFloat())

    return x.coerceIn(0.12f, 0.86f) to y.coerceIn(0.12f, 0.86f)
}
