package app.thdev.glassnavlab.feature.notmid.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
fun NotmidRouteDetailContent(
    routeTitle: String,
    routeSubtitle: String,
    routeMeta: String,
    primaryClip: NotmidClip,
    primaryPlace: NotmidPlace,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = NotmidTheme.spacing.screenHorizontal,
            top = NotmidTheme.spacing.screenTop,
            end = NotmidTheme.spacing.screenHorizontal,
            bottom = NotmidTheme.spacing.bottomNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "route-detail-header-$routeMeta") {
            NotmidSectionHeader(
                eyebrow = routeMeta,
                title = routeTitle,
                subtitle = routeSubtitle,
            )
        }

        if (actions != null) {
            item(key = "route-detail-actions-$routeMeta") {
                actions()
            }
        }

        item(key = "route-detail-clip-$routeMeta") {
            NotmidClipCard(clip = primaryClip)
        }

        item(key = "route-detail-place-$routeMeta") {
            NotmidPlaceCard(
                place = primaryPlace,
                index = 0,
            )
        }
    }
}
