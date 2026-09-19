package app.thdev.glassnavlab.feature.feed

import android.os.Bundle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.thdev.glassnavlab.core.designsystem.component.backdrop.rememberListBackdropColor
import app.thdev.glassnavlab.feature.feed.api.route.ClipDetailRoute

@Composable
fun ClipDetailScreen(
    route: ClipDetailRoute,
    isStartingChat: Boolean = false,
    onBackdropColorChanged: (Color) -> Unit = {},
) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val extras = remember(owner, route.clipId) {
        MutableCreationExtras((owner as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
            set(DEFAULT_ARGS_KEY, Bundle().apply { putString(ClipDetailViewModel.CLIP_ID, route.clipId) })
        }
    }
    val viewModel: ClipDetailViewModel = viewModel(key = route.route, extras = extras)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val palettes = (state as? ClipDetailUiState.Ready)?.backdropPalettes.orEmpty()
    val backdropColor by rememberListBackdropColor(listState, palettes)
    SideEffect { onBackdropColorChanged(backdropColor) }

    ClipDetailContent(
        state = state,
        listState = listState,
        isStartingChat = isStartingChat,
        onAction = viewModel::onAction,
    )
}
