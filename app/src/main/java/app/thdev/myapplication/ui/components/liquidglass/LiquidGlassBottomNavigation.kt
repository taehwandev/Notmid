package app.thdev.myapplication.ui.components.liquidglass

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.roundToInt

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
        items = items,
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

    val selectedIndex = items.indexOfFirst { it.id == selectedItemId }.coerceAtLeast(0)
    var gestureGlassActive by remember { mutableStateOf(false) }
    var selectionTransitionActive by remember { mutableStateOf(false) }
    var observedSelectedIndex by remember { mutableStateOf(selectedIndex) }
    var activeIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val glassActive = gestureGlassActive || selectionTransitionActive
    val glassProgress by animateFloatAsState(
        targetValue = if (glassActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "liquid selected glass progress",
    )
    val pillIndex by animateFloatAsState(
        targetValue = if (gestureGlassActive) activeIndex else selectedIndex.toFloat(),
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
        if (selectedIndex != observedSelectedIndex) {
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
                surfaceColor = colors.actionSurfaceColor,
                renderMode = renderMode,
                glassIntensity = 0f,
                baseBlur = 14.dp,
                activeBlur = 5.dp,
                refractionHeight = 8.dp,
                refractionAmount = 10.dp,
                chromaticAberration = true,
            ),
        contentAlignment = Alignment.Center,
    ) {
        action.icon(colors.actionContentColor)
    }
}

@Composable
private fun rememberLiquidGlassResolvedColors(
    style: LiquidGlassNavigationStyle,
    adaptiveBackgroundColor: Color,
): LiquidGlassResolvedColors {
    val tone = rememberLiquidGlassTone(adaptiveBackgroundColor)
    val target = resolveLiquidGlassColors(style, tone)
    val colorAnimationSpec = tween<Color>(durationMillis = 650)
    val containerSurfaceColor by animateColorAsState(
        targetValue = target.containerSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid container surface color",
    )
    val selectedSurfaceColor by animateColorAsState(
        targetValue = target.selectedSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid selected surface color",
    )
    val actionSurfaceColor by animateColorAsState(
        targetValue = target.actionSurfaceColor,
        animationSpec = colorAnimationSpec,
        label = "liquid action surface color",
    )
    val selectedContentColor by animateColorAsState(
        targetValue = target.selectedContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid selected content color",
    )
    val unselectedContentColor by animateColorAsState(
        targetValue = target.unselectedContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid unselected content color",
    )
    val actionContentColor by animateColorAsState(
        targetValue = target.actionContentColor,
        animationSpec = colorAnimationSpec,
        label = "liquid action content color",
    )

    return LiquidGlassResolvedColors(
        containerSurfaceColor = containerSurfaceColor,
        selectedSurfaceColor = selectedSurfaceColor,
        actionSurfaceColor = actionSurfaceColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        actionContentColor = actionContentColor,
    )
}

@Composable
private fun rememberLiquidGlassTone(adaptiveBackgroundColor: Color): LiquidGlassTone? {
    if (adaptiveBackgroundColor == Color.Unspecified) return null

    var tone by remember {
        mutableStateOf(toneForLuminance(adaptiveBackgroundColor.luminance()))
    }

    LaunchedEffect(adaptiveBackgroundColor) {
        val luminance = adaptiveBackgroundColor.luminance()
        tone = when (tone) {
            LiquidGlassTone.Light -> {
                if (luminance < DarkToneLuminanceThreshold) {
                    LiquidGlassTone.Dark
                } else {
                    LiquidGlassTone.Light
                }
            }

            LiquidGlassTone.Dark -> {
                if (luminance > LightToneLuminanceThreshold) {
                    LiquidGlassTone.Light
                } else {
                    LiquidGlassTone.Dark
                }
            }
        }
    }

    return tone
}

private fun resolveLiquidGlassColors(
    style: LiquidGlassNavigationStyle,
    tone: LiquidGlassTone?,
): LiquidGlassResolvedColors {
    if (tone == null) {
        return LiquidGlassResolvedColors(
            containerSurfaceColor = style.containerSurfaceColor,
            selectedSurfaceColor = style.selectedSurfaceColor,
            actionSurfaceColor = style.selectedSurfaceColor,
            selectedContentColor = style.selectedContentColor,
            unselectedContentColor = style.unselectedContentColor,
            actionContentColor = style.selectedContentColor,
        )
    }

    return when (tone) {
        LiquidGlassTone.Light -> LiquidGlassResolvedColors(
            containerSurfaceColor = Color.White.copy(alpha = 0.26f),
            selectedSurfaceColor = Color.White.copy(alpha = 0.42f),
            actionSurfaceColor = Color.White.copy(alpha = 0.30f),
            selectedContentColor = Color(0xFF111111),
            unselectedContentColor = Color(0xFF2F343A).copy(alpha = 0.52f),
            actionContentColor = Color(0xFF111111),
        )

        LiquidGlassTone.Dark -> LiquidGlassResolvedColors(
            containerSurfaceColor = Color(0xFF34414B).copy(alpha = 0.34f),
            selectedSurfaceColor = Color(0xFF7E8A94).copy(alpha = 0.38f),
            actionSurfaceColor = Color(0xFF65717B).copy(alpha = 0.36f),
            selectedContentColor = Color.White.copy(alpha = 0.98f),
            unselectedContentColor = Color(0xFFDCE6EF).copy(alpha = 0.54f),
            actionContentColor = Color.White.copy(alpha = 0.98f),
        )
    }
}

private data class LiquidGlassResolvedColors(
    val containerSurfaceColor: Color,
    val selectedSurfaceColor: Color,
    val actionSurfaceColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val actionContentColor: Color,
)

private enum class LiquidGlassTone {
    Light,
    Dark,
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

private fun toneForLuminance(luminance: Float): LiquidGlassTone {
    return if (luminance < InitialDarkToneLuminanceThreshold) {
        LiquidGlassTone.Dark
    } else {
        LiquidGlassTone.Light
    }
}

private const val InitialDarkToneLuminanceThreshold = 0.44f
private const val DarkToneLuminanceThreshold = 0.34f
private const val LightToneLuminanceThreshold = 0.58f

private suspend fun PointerInputScope.liquidTabGestureInput(
    itemsSize: Int,
    contentPaddingStartPx: Float,
    contentPaddingEndPx: Float,
    onGlassActiveChanged: (Boolean) -> Unit,
    onActiveIndexChanged: (Float) -> Unit,
    onVelocityChanged: (Float) -> Unit,
    onItemSelected: (Int) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tabWidthPx = ((size.width - contentPaddingStartPx - contentPaddingEndPx) / itemsSize)
            .coerceAtLeast(1f)

        fun indexFor(x: Float): Float {
            return ((x - contentPaddingStartPx) / tabWidthPx - 0.5f)
                .coerceIn(0f, (itemsSize - 1).toFloat())
        }

        var activeIndex = indexFor(down.position.x)
        var selectedIndex = activeIndex.roundToInt().coerceIn(0, itemsSize - 1)
        var lastX = down.position.x
        var movedPastSlop = false
        var pointerUp = false

        fun processChange(change: PointerInputChange) {
            val nextIndex = indexFor(change.position.x)
            val nextSelectedIndex = nextIndex.roundToInt().coerceIn(0, itemsSize - 1)
            val frameVelocity = ((change.position.x - lastX) / tabWidthPx).coerceIn(-1f, 1f)

            activeIndex = nextIndex
            lastX = change.position.x
            onActiveIndexChanged(activeIndex)
            onVelocityChanged(frameVelocity)

            if (nextSelectedIndex != selectedIndex) {
                selectedIndex = nextSelectedIndex
                onItemSelected(selectedIndex)
            }

            if (change.positionChange().x != 0f || change.positionChange().y != 0f) {
                change.consume()
            }
        }

        onActiveIndexChanged(activeIndex)
        onItemSelected(selectedIndex)
        onGlassActiveChanged(false)
        onVelocityChanged(0f)

        try {
            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                        pointerUp = true
                        break
                    }

                    val totalDx = change.position.x - down.position.x
                    val totalDy = change.position.y - down.position.y
                    if (!movedPastSlop &&
                        (abs(totalDx) > viewConfiguration.touchSlop ||
                            abs(totalDy) > viewConfiguration.touchSlop)
                    ) {
                        movedPastSlop = true
                        onGlassActiveChanged(true)
                        break
                    }

                    processChange(change)
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            if (!movedPastSlop && !pointerUp) {
                onGlassActiveChanged(true)
            }
        }

        while (!pointerUp) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                pointerUp = true
                break
            }

            processChange(change)
        }

        onGlassActiveChanged(false)
        onVelocityChanged(0f)
    }
}

private suspend fun PointerInputScope.liquidActionButtonInput(
    onPressedChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPressedChanged(true)

        var releasedInside = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                releasedInside = change.position.x in 0f..size.width.toFloat() &&
                    change.position.y in 0f..size.height.toFloat()
                break
            }

            if (change.positionChange().x != 0f || change.positionChange().y != 0f) {
                change.consume()
            }
        }

        onPressedChanged(false)
        if (releasedInside) {
            onClick()
        }
    }
}

private fun shouldUseLens(renderMode: LiquidGlassRenderMode): Boolean {
    return renderMode != LiquidGlassRenderMode.Legacy &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

private fun resolveSelectedItemId(
    items: List<LiquidGlassNavigationItem>,
    state: LiquidGlassNavigationState,
): String {
    return if (items.any { it.id == state.selectedItemId }) {
        state.selectedItemId
    } else {
        items.firstOrNull()?.id.orEmpty()
    }
}
