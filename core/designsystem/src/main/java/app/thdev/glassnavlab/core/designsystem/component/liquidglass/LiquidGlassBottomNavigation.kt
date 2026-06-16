package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

@Composable
fun LiquidGlassBottomNavigation(
    items: List<LiquidGlassNavigationItem>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    state: LiquidGlassNavigationState = rememberLiquidGlassNavigationState(
        initialSelectedItemId = items.firstOrNull()?.id.orEmpty(),
    ),
    style: LiquidGlassNavigationStyle = LiquidGlassNavigationDefaults.style(),
    renderMode: LiquidGlassRenderMode = LiquidGlassRenderMode.Automatic,
    adaptiveBackgroundColor: Color = Color.Unspecified,
    trailingAction: LiquidGlassNavigationAction? = null,
    onItemSelected: (LiquidGlassNavigationItem) -> Unit = {},
    itemContent: @Composable (
        item: LiquidGlassNavigationItem,
        selected: Boolean,
        contentColor: Color,
    ) -> Unit = { item, selected, contentColor ->
        LiquidGlassNavigationItemContent(
            item = item,
            selected = selected,
            contentColor = contentColor,
        )
    },
) {
    val selectedItemId = resolveSelectedItemId(
        state = state,
    )

    LiquidGlassBottomNavigationBar(
        items = items,
        selectedItemId = selectedItemId,
        backdrop = backdrop,
        onItemSelected = { item ->
            state.select(item.id)
            onItemSelected(item)
        },
        modifier = modifier,
        style = style,
        renderMode = renderMode,
        adaptiveBackgroundColor = adaptiveBackgroundColor,
        trailingAction = trailingAction,
        itemContent = itemContent,
    )
}

@Composable
fun LiquidGlassBottomNavigationBar(
    items: List<LiquidGlassNavigationItem>,
    selectedItemId: String,
    backdrop: Backdrop,
    onItemSelected: (LiquidGlassNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
    style: LiquidGlassNavigationStyle = LiquidGlassNavigationDefaults.style(),
    renderMode: LiquidGlassRenderMode = LiquidGlassRenderMode.Automatic,
    adaptiveBackgroundColor: Color = Color.Unspecified,
    trailingAction: LiquidGlassNavigationAction? = null,
    itemContent: @Composable (
        item: LiquidGlassNavigationItem,
        selected: Boolean,
        contentColor: Color,
    ) -> Unit = { item, selected, contentColor ->
        LiquidGlassNavigationItemContent(
            item = item,
            selected = selected,
            contentColor = contentColor,
        )
    },
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.id == selectedItemId }
    val motionSelectedIndex = selectedIndex.coerceAtLeast(0)
    var gestureGlassActive by remember { mutableStateOf(false) }
    var selectionTransitionActive by remember { mutableStateOf(false) }
    var observedSelectedIndex by remember { mutableStateOf(motionSelectedIndex) }
    var activeIndex by remember { mutableFloatStateOf(motionSelectedIndex.toFloat()) }
    val glassActive = gestureGlassActive || selectionTransitionActive
    val glassProgress by animateFloatAsState(
        targetValue = if (glassActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "liquid selected glass progress",
    )
    val pillIndex by animateFloatAsState(
        targetValue = if (gestureGlassActive) activeIndex else motionSelectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 650f),
        label = "liquid selected index",
    )
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val colors = rememberLiquidGlassResolvedColors(
        style = style,
        adaptiveBackgroundColor = adaptiveBackgroundColor,
    )

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex != observedSelectedIndex) {
            observedSelectedIndex = selectedIndex
            selectionTransitionActive = true
            delay(520)
            selectionTransitionActive = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(style.outerPadding)
            .height(style.height),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(style.height),
            horizontalArrangement = Arrangement.spacedBy(style.actionButtonSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(style.height),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val contentPaddingStart = style.contentPadding.calculateLeftPadding(layoutDirection)
                val contentPaddingEnd = style.contentPadding.calculateRightPadding(layoutDirection)
                val contentWidth = maxWidth - contentPaddingStart - contentPaddingEnd
                val tabWidth = contentWidth / items.size
                val pillOffset = tabWidth * pillIndex
                val barWidth = maxWidth

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(style.height)
                        .pointerInput(
                            items.map { it.id },
                            contentPaddingStart,
                            contentPaddingEnd,
                            density,
                        ) {
                            liquidTabGestureInput(
                                itemsSize = items.size,
                                contentPaddingStartPx = with(density) { contentPaddingStart.toPx() },
                                contentPaddingEndPx = with(density) { contentPaddingEnd.toPx() },
                                onGlassActiveChanged = { gestureGlassActive = it },
                                onActiveIndexChanged = { activeIndex = it },
                                onVelocityChanged = {},
                                onItemSelected = { index -> onItemSelected(items[index]) },
                            )
                        }
                        .liquidGlassSurface(
                            backdrop = backdrop,
                            shape = { style.containerShape },
                            surfaceColor = colors.containerSurfaceColor,
                            renderMode = renderMode,
                            glassIntensity = glassProgress,
                            baseBlur = 18.dp,
                            activeBlur = 7.dp,
                            refractionHeight = 12.dp,
                            refractionAmount = 16.dp,
                        )
                ) {
                    val selectedPillWidth = minOf(
                        tabWidth * style.selectedPillWidthFraction,
                        contentWidth,
                    )
                    val selectedPillX = (
                        contentPaddingStart + pillOffset + (tabWidth - selectedPillWidth) / 2f
                    ).coerceIn(0.dp, barWidth - selectedPillWidth)
                    if (selectedIndex >= 0) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = selectedPillX,
                                    y = style.contentPadding.calculateTopPadding() +
                                        (style.itemHeight - style.selectedPillHeight) / 2f,
                                )
                                .width(selectedPillWidth)
                                .height(style.selectedPillHeight)
                                .liquidGlassSurface(
                                    backdrop = backdrop,
                                    shape = { style.itemShape },
                                    surfaceColor = colors.selectedSurfaceColor,
                                    renderMode = renderMode,
                                    glassIntensity = glassProgress,
                                    activeBlur = 5.dp,
                                    refractionHeight = 9.dp,
                                    refractionAmount = 12.dp,
                                    chromaticAberration = true,
                                    flatWhenIdle = true,
                                    layerBlock = {
                                        val scale = lerp(1f, 1.03f, glassProgress)
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(style.contentPadding),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEach { item ->
                            val selected = item.id == selectedItemId
                            val contentColor = if (selected) {
                                colors.selectedContentColor
                            } else {
                                colors.unselectedContentColor
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(style.itemHeight),
                                contentAlignment = Alignment.Center,
                            ) {
                                itemContent(item, selected, contentColor)
                            }
                        }
                    }
                }
            }

            if (trailingAction != null) {
                LiquidGlassNavigationActionButton(
                    action = trailingAction,
                    backdrop = backdrop,
                    style = style,
                    renderMode = renderMode,
                    colors = colors,
                    modifier = Modifier.size(style.actionButtonSize),
                )
            }
        }
    }
}

@Composable
fun LiquidGlassNavigationActionButton(
    action: LiquidGlassNavigationAction,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    style: LiquidGlassNavigationStyle = LiquidGlassNavigationDefaults.style(),
    renderMode: LiquidGlassRenderMode = LiquidGlassRenderMode.Automatic,
    adaptiveBackgroundColor: Color = Color.Unspecified,
) {
    val colors = rememberLiquidGlassResolvedColors(
        style = style,
        adaptiveBackgroundColor = adaptiveBackgroundColor,
    )

    LiquidGlassNavigationActionButton(
        action = action,
        backdrop = backdrop,
        style = style,
        renderMode = renderMode,
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun LiquidGlassNavigationActionButton(
    action: LiquidGlassNavigationAction,
    backdrop: Backdrop,
    style: LiquidGlassNavigationStyle,
    renderMode: LiquidGlassRenderMode,
    colors: LiquidGlassResolvedColors,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "liquid action press scale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = action.contentDescription
                role = Role.Button
                onClick {
                    action.onClick()
                    true
                }
            }
            .pointerInput(action.onClick) {
                liquidActionButtonInput(
                    onPressedChanged = { pressed = it },
                    onClick = action.onClick,
                )
            }
            .liquidGlassSurface(
                backdrop = backdrop,
                shape = { style.actionButtonShape },
                surfaceColor = if (action.selected) {
                    colors.selectedSurfaceColor
                } else {
                    colors.actionSurfaceColor
                },
                renderMode = renderMode,
                glassIntensity = if (action.selected) 0.35f else 0f,
                baseBlur = 14.dp,
                activeBlur = 5.dp,
                refractionHeight = 8.dp,
                refractionAmount = 10.dp,
                chromaticAberration = true,
            ),
        contentAlignment = Alignment.Center,
    ) {
        action.icon(
            if (action.selected) {
                colors.selectedContentColor
            } else {
                colors.actionContentColor
            },
        )
    }
}

private fun Modifier.liquidGlassSurface(
    backdrop: Backdrop,
    shape: () -> Shape,
    surfaceColor: Color,
    renderMode: LiquidGlassRenderMode,
    glassIntensity: Float,
    baseBlur: Dp = 0.dp,
    activeBlur: Dp = 0.dp,
    refractionHeight: Dp = 0.dp,
    refractionAmount: Dp = 0.dp,
    chromaticAberration: Boolean = false,
    flatWhenIdle: Boolean = false,
    layerBlock: GraphicsLayerScope.() -> Unit = {},
): Modifier {
    val progress = glassIntensity.coerceIn(0f, 1f)
    if (flatWhenIdle && progress <= 0.01f) {
        return clip(shape()).background(surfaceColor)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            val blurRadius = baseBlur.toPx() + activeBlur.toPx() * progress
            if (blurRadius > 0f) {
                blur(blurRadius)
            }

            if (progress > 0.01f && shouldUseLens(renderMode)) {
                vibrancy()
                lens(
                    refractionHeight = refractionHeight.toPx() * progress,
                    refractionAmount = refractionAmount.toPx() * progress,
                    depthEffect = true,
                    chromaticAberration = chromaticAberration,
                )
            }

        },
        layerBlock = layerBlock,
        onDrawSurface = {
            drawRect(surfaceColor)
            drawLiquidGlassAgslOverlay(
                shape = shape(),
                surfaceColor = surfaceColor,
                renderMode = renderMode,
                progress = progress,
            )
        },
    )
}

@Composable
private fun LiquidGlassNavigationItemContent(
    item: LiquidGlassNavigationItem,
    selected: Boolean,
    contentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item.icon(selected, contentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            color = contentColor,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun shouldUseLens(renderMode: LiquidGlassRenderMode): Boolean {
    return renderMode != LiquidGlassRenderMode.Legacy &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

private fun resolveSelectedItemId(state: LiquidGlassNavigationState): String {
    return state.selectedItemId
}
