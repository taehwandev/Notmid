package app.thdev.glassnavlab.feature.notmid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidBottomNavigation
import app.thdev.glassnavlab.core.designsystem.component.NotmidBottomNavigationItem
import app.thdev.glassnavlab.core.designsystem.component.liquidglass.LiquidGlassNavigationAction
import app.thdev.glassnavlab.feature.notmid.api.NotmidDestinationIds
import app.thdev.glassnavlab.feature.notmid.api.NotmidRouteEvent
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidGlassIcon
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import com.kyant.backdrop.Backdrop

@Composable
internal fun NotmidShellBottomNavigation(
    destinations: List<NotmidDestination>,
    selectedDestinationId: String,
    navigationBackdropColor: Color,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onRouteEvent: (NotmidRouteEvent) -> Unit,
) {
    NotmidBottomNavigation(
        items = rememberNotmidNavigationItems(destinations),
        selectedItemId = selectedDestinationId,
        backdrop = backdrop,
        modifier = modifier,
        adaptiveBackgroundColor = navigationBackdropColor,
        trailingAction = LiquidGlassNavigationAction(
            contentDescription = "Create clip",
            icon = { color -> NotmidPlusIcon(color) },
            selected = selectedDestinationId == NotmidDestinationIds.CAPTURE,
            onClick = {
                onRouteEvent(
                    NotmidRouteEvent.DestinationSelected(
                        NotmidDestinationIds.CAPTURE,
                    ),
                )
            },
        ),
        onItemSelected = { item ->
            onRouteEvent(NotmidRouteEvent.DestinationSelected(item.id))
        },
    )
}

@Composable
private fun rememberNotmidNavigationItems(
    destinations: List<NotmidDestination>,
): List<NotmidBottomNavigationItem> {
    return remember(destinations) {
        destinations.filterNot { destination ->
            destination.id == NotmidDestinationIds.CAPTURE
        }.map { destination ->
            NotmidBottomNavigationItem(
                id = destination.id,
                label = destination.title,
                icon = { _, color -> NotmidGlassIcon(destination.icon, color) },
            )
        }
    }
}

@Composable
private fun NotmidPlusIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 2.4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.50f, size.height * 0.24f),
            end = Offset(size.width * 0.50f, size.height * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.24f, size.height * 0.50f),
            end = Offset(size.width * 0.76f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
