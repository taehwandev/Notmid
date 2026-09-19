package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidActionRail
import app.thdev.glassnavlab.core.designsystem.component.NotmidActionRailItem

@Composable
internal fun FeedRail(
    clip: FeedClipUi,
    modifier: Modifier = Modifier,
    onClipSelected: (String) -> Unit,
) {
    val items = remember(clip.id, clip.likeCountLabel, clip.saveCountLabel, clip.chatCountLabel, onClipSelected) {
        listOf(
            FeedRailAction.Like.toItem(
                value = clip.likeCountLabel,
                onClick = { onClipSelected(clip.id) },
            ),
            FeedRailAction.Save.toItem(
                value = clip.saveCountLabel,
                onClick = { onClipSelected(clip.id) },
            ),
            FeedRailAction.Chat.toItem(
                value = clip.chatCountLabel,
                onClick = { onClipSelected(clip.id) },
            ),
            FeedRailAction.Share.toItem(
                value = "share",
                onClick = { onClipSelected(clip.id) },
            ),
        )
    }

    NotmidActionRail(
        items = items,
        modifier = modifier,
        darkOnMedia = true,
    )
}

private enum class FeedRailAction(
    val label: String,
) {
    Like("Like"),
    Save("Save"),
    Chat("Chat"),
    Share("Share");

    fun toItem(
        value: String,
        onClick: () -> Unit,
    ): NotmidActionRailItem {
        return NotmidActionRailItem(
            id = name,
            label = label,
            value = value,
            icon = { color -> FeedRailIcon(action = this, color = color) },
            onClick = onClick,
        )
    }
}

@Composable
private fun FeedRailIcon(
    action: FeedRailAction,
    color: Color,
) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.dp.toPx())
        when (action) {
            FeedRailAction.Like -> {
                val heart = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.78f)
                    cubicTo(
                        size.width * 0.16f,
                        size.height * 0.56f,
                        size.width * 0.18f,
                        size.height * 0.24f,
                        size.width * 0.42f,
                        size.height * 0.26f,
                    )
                    cubicTo(
                        size.width * 0.50f,
                        size.height * 0.28f,
                        size.width * 0.50f,
                        size.height * 0.36f,
                        size.width * 0.50f,
                        size.height * 0.36f,
                    )
                    cubicTo(
                        size.width * 0.50f,
                        size.height * 0.36f,
                        size.width * 0.50f,
                        size.height * 0.28f,
                        size.width * 0.58f,
                        size.height * 0.26f,
                    )
                    cubicTo(
                        size.width * 0.82f,
                        size.height * 0.24f,
                        size.width * 0.84f,
                        size.height * 0.56f,
                        size.width * 0.50f,
                        size.height * 0.78f,
                    )
                }
                drawPath(heart, color = color, style = stroke)
            }

            FeedRailAction.Save -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.30f, size.height * 0.18f),
                    size = Size(size.width * 0.40f, size.height * 0.64f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.30f, size.height * 0.80f),
                    end = Offset(size.width * 0.50f, size.height * 0.62f),
                    strokeWidth = stroke.width,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.70f, size.height * 0.80f),
                    end = Offset(size.width * 0.50f, size.height * 0.62f),
                    strokeWidth = stroke.width,
                )
            }

            FeedRailAction.Chat -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.20f, size.height * 0.24f),
                    size = Size(size.width * 0.60f, size.height * 0.44f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.40f, size.height * 0.68f),
                    end = Offset(size.width * 0.34f, size.height * 0.82f),
                    strokeWidth = stroke.width,
                )
            }

            FeedRailAction.Share -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.28f, size.height * 0.58f),
                    end = Offset(size.width * 0.72f, size.height * 0.30f),
                    strokeWidth = stroke.width,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.28f, size.height * 0.58f),
                    end = Offset(size.width * 0.72f, size.height * 0.76f),
                    strokeWidth = stroke.width,
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.08f,
                    center = Offset(size.width * 0.28f, size.height * 0.58f),
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.08f,
                    center = Offset(size.width * 0.72f, size.height * 0.30f),
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.08f,
                    center = Offset(size.width * 0.72f, size.height * 0.76f),
                )
            }
        }
    }
}
