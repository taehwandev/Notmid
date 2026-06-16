package app.thdev.glassnavlab.core.designsystem.component.liquidglass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.roundToInt

internal suspend fun PointerInputScope.liquidTabGestureInput(
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

internal suspend fun PointerInputScope.liquidActionButtonInput(
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
