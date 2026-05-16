package app.thdev.myapplication.ui.demo

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.myapplication.ui.components.liquidglass.LiquidGlassBackdropHost
import app.thdev.myapplication.ui.components.liquidglass.LiquidGlassBottomNavigation
import app.thdev.myapplication.ui.components.liquidglass.LiquidGlassNavigationAction
import app.thdev.myapplication.ui.components.liquidglass.LiquidGlassNavigationItem
import app.thdev.myapplication.ui.components.liquidglass.rememberLiquidGlassNavigationState

@Composable
fun LiquidGlassDemoScreen() {
    val items = rememberDemoNavigationItems()
    val navigationState = rememberLiquidGlassNavigationState(
        initialSelectedItemId = items.first().id,
    )
    val selectedDestination = destinationFor(navigationState.selectedItemId)
    val homeListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val createListState = rememberLazyListState()
    val profileListState = rememberLazyListState()
    val listState = when (selectedDestination.id) {
        "search" -> searchListState
        "create" -> createListState
        "profile" -> profileListState
        else -> homeListState
    }
    val navigationBackdropColor by rememberNavigationBackdropColor(
        listState = listState,
        destination = selectedDestination,
    )

    LiquidGlassBackdropHost(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoBackgroundColor),
        backgroundColor = DemoBackgroundColor,
        content = {
            DemoContent(
                destination = selectedDestination,
                listState = listState,
            )
        },
        floatingContent = { backdrop ->
            LiquidGlassBottomNavigation(
                items = items,
                state = navigationState,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter),
                adaptiveBackgroundColor = navigationBackdropColor,
                trailingAction = LiquidGlassNavigationAction(
                    contentDescription = "Create",
                    icon = { color -> DemoGlassIcon(GlassNavIcon.Create, color) },
                    onClick = { navigationState.select("create") },
                ),
            )
        },
    )
}

@Composable
private fun rememberDemoNavigationItems(): List<LiquidGlassNavigationItem> {
    return remember {
        listOf(
            LiquidGlassNavigationItem(
                id = "home",
                label = "Home",
                icon = { _, color -> DemoGlassIcon(GlassNavIcon.Home, color) },
            ),
            LiquidGlassNavigationItem(
                id = "search",
                label = "Search",
                icon = { _, color -> DemoGlassIcon(GlassNavIcon.Search, color) },
            ),
            LiquidGlassNavigationItem(
                id = "create",
                label = "Create",
                icon = { _, color -> DemoGlassIcon(GlassNavIcon.Create, color) },
            ),
            LiquidGlassNavigationItem(
                id = "profile",
                label = "Profile",
                icon = { _, color -> DemoGlassIcon(GlassNavIcon.Profile, color) },
            ),
        )
    }
}

@Composable
private fun DemoContent(
    destination: DemoDestination,
    listState: LazyListState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 64.dp,
            end = 20.dp,
            bottom = 150.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header-${destination.id}") {
            DemoHeader(destination)
        }

        itemsIndexed(
            items = destination.surfaces,
            key = { index, surface -> "${destination.id}-${surface.title}-$index" },
        ) { index, surface ->
            DemoSurfaceCard(
                surface = surface,
                index = index,
            )
        }
    }
}

@Composable
private fun DemoHeader(destination: DemoDestination) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Glass Nav Lab",
                color = Color(0xFF171717),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = destination.subtitle,
                color = Color(0xFF575757),
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DemoStatusTile(
                label = "Renderer",
                value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "AGSL"
                } else {
                    "Legacy"
                },
                modifier = Modifier.weight(1f),
            )
            DemoStatusTile(
                label = "Example",
                value = destination.title,
                modifier = Modifier.weight(1f),
            )
            DemoStatusTile(
                label = "Items",
                value = destination.surfaces.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DemoStatusTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            color = Color(0xFF6B6F74),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            color = Color(0xFF171717),
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun rememberNavigationBackdropColor(
    listState: LazyListState,
    destination: DemoDestination,
): State<Color> {
    val density = LocalDensity.current
    val sampleOffsetFromBottom = with(density) { 88.dp.roundToPx() }

    return remember(listState, destination.id, sampleOffsetFromBottom) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val sampleY = layoutInfo.viewportEndOffset - sampleOffsetFromBottom
            val visibleCard = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                item.index > 0 && sampleY >= item.offset && sampleY <= item.offset + item.size
            } ?: return@derivedStateOf DemoBackgroundColor

            val surface = destination.surfaces.getOrNull(visibleCard.index - 1)
                ?: return@derivedStateOf DemoBackgroundColor
            val localFraction = ((sampleY - visibleCard.offset).toFloat() / visibleCard.size)
                .coerceIn(0f, 1f)
            sampleDemoPalette(
                palette = surface.palette,
                fraction = localFraction,
            )
        }
    }
}

private fun sampleDemoPalette(
    palette: List<Color>,
    fraction: Float,
): Color {
    if (palette.isEmpty()) return DemoBackgroundColor
    if (palette.size == 1) return palette.first()

    val scaledFraction = fraction.coerceIn(0f, 1f) * palette.lastIndex
    val startIndex = scaledFraction.toInt().coerceIn(0, palette.lastIndex - 1)
    val endIndex = startIndex + 1
    return lerpColor(
        start = palette[startIndex],
        stop = palette[endIndex],
        fraction = scaledFraction - startIndex,
    )
}

@Composable
private fun DemoSurfaceCard(
    surface: DemoSurface,
    index: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(surface.height)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(surface.palette))
            .padding(22.dp),
    ) {
        Text(
            text = surface.metric,
            modifier = Modifier.align(Alignment.TopEnd),
            color = surface.contentColor.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = surface.title,
                color = surface.contentColor,
                fontSize = if (index % 3 == 1) 25.sp else 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = surface.description,
                color = surface.contentColor.copy(alpha = 0.76f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun DemoGlassIcon(type: GlassNavIcon, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(
            width = 2.1.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (type) {
            GlassNavIcon.Home -> {
                val roof = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.48f)
                    lineTo(size.width * 0.50f, size.height * 0.22f)
                    lineTo(size.width * 0.82f, size.height * 0.48f)
                }
                val body = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.46f)
                    lineTo(size.width * 0.28f, size.height * 0.78f)
                    lineTo(size.width * 0.72f, size.height * 0.78f)
                    lineTo(size.width * 0.72f, size.height * 0.46f)
                }
                drawPath(roof, color, style = stroke)
                drawPath(body, color, style = stroke)
            }

            GlassNavIcon.Search -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.26f,
                    center = Offset(size.width * 0.43f, size.height * 0.42f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.61f, size.height * 0.61f),
                    end = Offset(size.width * 0.80f, size.height * 0.80f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            GlassNavIcon.Create -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.50f, size.height * 0.22f),
                    end = Offset(size.width * 0.50f, size.height * 0.78f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.22f, size.height * 0.50f),
                    end = Offset(size.width * 0.78f, size.height * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            GlassNavIcon.Profile -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.16f,
                    center = Offset(size.width * 0.50f, size.height * 0.35f),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.26f, size.height * 0.52f),
                    size = Size(size.width * 0.48f, size.height * 0.38f),
                    style = stroke,
                )
            }
        }
    }
}

private fun destinationFor(selectedItemId: String): DemoDestination {
    return DemoDestinations.firstOrNull { it.id == selectedItemId } ?: DemoDestinations.first()
}

private data class DemoDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val surfaces: List<DemoSurface>,
)

private data class DemoSurface(
    val title: String,
    val description: String,
    val metric: String,
    val palette: List<Color>,
    val height: Dp,
    val contentColor: Color = Color.White,
)

private enum class GlassNavIcon {
    Home,
    Search,
    Create,
    Profile,
}

private val DemoBackgroundColor = Color(0xFFF3F1EC)
private val DarkCardContent = Color(0xFF17202A)

private val DemoDestinations = listOf(
    DemoDestination(
        id = "home",
        title = "Home",
        subtitle = "A richer feed for checking the navigation over changing surfaces.",
        surfaces = listOf(
            DemoSurface(
                title = "Adaptive Surface",
                description = "The bar samples the lower viewport and switches between two readable tones.",
                metric = "Tone",
                palette = listOf(Color(0xFFFA705A), Color(0xFF7A4EF3), Color(0xFF232A7C)),
                height = 180.dp,
            ),
            DemoSurface(
                title = "Shared Action",
                description = "The trailing plus button uses the same glass language as the main bar.",
                metric = "Action",
                palette = listOf(Color(0xFF0E7C66), Color(0xFF32D6A2), Color(0xFFF6D66B)),
                height = 138.dp,
            ),
            DemoSurface(
                title = "Dark Backdrop",
                description = "Dark content keeps the menu readable without forcing a full black overlay.",
                metric = "Dark",
                palette = listOf(Color(0xFF101820), Color(0xFF31495E), Color(0xFFC8D7E0)),
                height = 154.dp,
            ),
            DemoSurface(
                title = "Bright Backdrop",
                description = "Bright content fades the inactive labels so the selected item stays dominant.",
                metric = "Bright",
                palette = listOf(Color(0xFF2F6BFF), Color(0xFF7FE3FF), Color(0xFFFFFFFF)),
                height = 132.dp,
                contentColor = DarkCardContent,
            ),
        ),
    ),
    DemoDestination(
        id = "search",
        title = "Search",
        subtitle = "A search-oriented example with result states behind the same glass navigation.",
        surfaces = listOf(
            DemoSurface(
                title = "Recent Queries",
                description = "Dense list content stays legible while the bottom bar floats above it.",
                metric = "12",
                palette = listOf(Color(0xFF262626), Color(0xFF8D8D8D), Color(0xFFF3F3F3)),
                height = 156.dp,
            ),
            DemoSurface(
                title = "Result Cluster",
                description = "Grouped content lets the bar prove its blur and refraction on mixed color.",
                metric = "48",
                palette = listOf(Color(0xFF005E7C), Color(0xFF5AD7CF), Color(0xFFF7E37B)),
                height = 180.dp,
            ),
            DemoSurface(
                title = "Focused Match",
                description = "High contrast foregrounds make selection state easy to compare.",
                metric = "Live",
                palette = listOf(Color(0xFF111827), Color(0xFF3B4A6B), Color(0xFFB9C7D6)),
                height = 124.dp,
            ),
            DemoSurface(
                title = "Saved Filter",
                description = "The component API stays stateless when the parent owns selectedItemId.",
                metric = "API",
                palette = listOf(Color(0xFFF65868), Color(0xFFB15FF4), Color(0xFF593AE8)),
                height = 148.dp,
            ),
        ),
    ),
    DemoDestination(
        id = "create",
        title = "Create",
        subtitle = "A creation flow showing flat settled selection and glass motion states.",
        surfaces = listOf(
            DemoSurface(
                title = "Draft Setup",
                description = "The selected pill rests flat, then becomes glass while moving.",
                metric = "01",
                palette = listOf(Color(0xFFEB4965), Color(0xFFF7A35C), Color(0xFFFFE4AA)),
                height = 150.dp,
            ),
            DemoSurface(
                title = "Media Stack",
                description = "Large vivid blocks reveal whether backdrop sampling reacts naturally.",
                metric = "02",
                palette = listOf(Color(0xFF2D5BE3), Color(0xFF26D0CE), Color(0xFFF1F7B5)),
                height = 190.dp,
                contentColor = DarkCardContent,
            ),
            DemoSurface(
                title = "Review State",
                description = "The bar keeps controls visible over mid-tone gradients.",
                metric = "03",
                palette = listOf(Color(0xFF313B72), Color(0xFF9C5FD5), Color(0xFFE9B5CA)),
                height = 132.dp,
            ),
            DemoSurface(
                title = "Publish Ready",
                description = "A separate round action can trigger the same destination.",
                metric = "04",
                palette = listOf(Color(0xFF0D3B2E), Color(0xFF1B9C85), Color(0xFFCCE8CC)),
                height = 156.dp,
            ),
        ),
    ),
    DemoDestination(
        id = "profile",
        title = "Profile",
        subtitle = "A profile dashboard example with neutral, dark, and bright backdrop tests.",
        surfaces = listOf(
            DemoSurface(
                title = "Usage Snapshot",
                description = "Neutral cards make inactive label contrast easier to judge.",
                metric = "87%",
                palette = listOf(Color(0xFF111111), Color(0xFF555555), Color(0xFFEDEDED)),
                height = 154.dp,
            ),
            DemoSurface(
                title = "Session Quality",
                description = "The AGSL sheen remains subtle over grayscale content.",
                metric = "A",
                palette = listOf(Color(0xFF3D4D57), Color(0xFF99AAB5), Color(0xFFE5ECEF)),
                height = 130.dp,
                contentColor = DarkCardContent,
            ),
            DemoSurface(
                title = "Saved Layout",
                description = "Component styling is centralized in LiquidGlassNavigationStyle.",
                metric = "Style",
                palette = listOf(Color(0xFF6930C3), Color(0xFF64DFDF), Color(0xFFFFF3B0)),
                height = 176.dp,
            ),
            DemoSurface(
                title = "Release Check",
                description = "Screenshots and video are kept in docs/assets for GitHub.",
                metric = "Docs",
                palette = listOf(Color(0xFF172A3A), Color(0xFF2E8BC0), Color(0xFFB1D4E0)),
                height = 144.dp,
            ),
        ),
    ),
)
