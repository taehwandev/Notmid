package app.thdev.glassnavlab.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@Composable
internal fun FeedContent(
    state: FeedUiState,
    listState: LazyListState,
    onAction: (FeedAction) -> Unit,
) {
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
        item(key = "feed-header") {
            NotmidSectionHeader(
                title = "See a place before you go",
                subtitle = "Place videos, map context, and a way to plan together.",
                eyebrow = state.title,
            )
        }

        item(key = "feed-hero-${state.heroClip?.id ?: "empty"}") {
            val clip = state.heroClip
            if (clip == null) {
                FeedEmptyStage()
            } else {
                FeedHeroStage(
                    clip = clip,
                    place = state.placeFor(clip),
                    onAction = onAction,
                )
            }
        }

        if (state.queue.isNotEmpty()) {
            item(key = "feed-queue-header") {
                NotmidSectionHeader(
                    title = "More place posts",
                    subtitle = "Recent posts linked to places.",
                    eyebrow = "places",
                )
            }

            items(
                items = state.queue,
                key = { clip -> "feed-queue-${clip.id}" },
            ) { clip ->
                FeedQueueCard(
                    clip = clip,
                    place = state.placeFor(clip),
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun FeedEmptyStage() {
    NotmidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLightStrong,
        contentPadding = PaddingValues(NotmidTheme.spacing.xl),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NotmidText(
                text = "No place posts yet",
                variant = NotmidTextVariant.Title,
            )
            Spacer(modifier = Modifier.height(NotmidTheme.spacing.sm))
            NotmidText(
                text = "When someone shares a place video, it will appear here.",
                color = NotmidTheme.colors.contentMuted,
                variant = NotmidTextVariant.BodySmall,
            )
        }
    }
}
