package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidClipCard
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.profile.components.feedback.ProfileEmptyState

@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    listState: LazyListState,
    onAction: (ProfileAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = ProfileContentPadding,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "profile-header") {
            ProfileHeader(
                user = state.auth.session?.user,
                onSettingsRequested = { onAction(ProfileAction.OpenSettings) },
            )
        }

        item(key = "profile-tabs") {
            ProfileTabs(
                selectedTab = state.selectedTab,
                onTabSelected = { onAction(ProfileAction.SelectTab(it)) },
            )
        }

        if (!state.auth.isAuthenticated) {
            item(key = "profile-signed-out") {
                ProfileEmptyState(
                    title = "Sign in to see your profile",
                    description = "Your clips, saves, places, and routes will appear here after you sign in.",
                )
            }
        } else {
            when (state.selectedTab) {
                ProfileTab.Clips -> profileClips(state.ownClips)
                ProfileTab.Saved -> profileTabEmpty(
                    tab = ProfileTab.Saved,
                    title = "Saved clips aren't available yet",
                    description = "Clips you save will appear here when saving is available.",
                )
                ProfileTab.Places -> profileTabEmpty(
                    tab = ProfileTab.Places,
                    title = "Saved places aren't available yet",
                    description = "Places you save will appear here when saving is available.",
                )
                ProfileTab.Routes -> profileTabEmpty(
                    tab = ProfileTab.Routes,
                    title = "Routes aren't available yet",
                    description = "Routes you plan will appear here when they can be saved.",
                )
            }
        }
    }
}

internal val ProfileContentPadding: PaddingValues
    @Composable get() = PaddingValues(
        start = NotmidTheme.spacing.screenHorizontal,
        top = NotmidTheme.spacing.screenTop,
        end = NotmidTheme.spacing.screenHorizontal,
        bottom = NotmidTheme.spacing.bottomNavigationPadding,
    )

private fun LazyListScope.profileClips(clips: List<NotmidClip>) {
    if (clips.isEmpty()) {
        profileTabEmpty(
            tab = ProfileTab.Clips,
            title = "No clips to show",
            description = "Clips posted with this account will appear here when profile clips are available.",
        )
        return
    }

    item(key = "profile-clips-header") {
        NotmidSectionHeader(
            title = "My clips",
            subtitle = "Clips posted with this account.",
            eyebrow = "clips",
        )
    }
    items(
        items = clips,
        key = { clip -> "profile-clip-${clip.id}" },
    ) { clip ->
        NotmidClipCard(clip = clip)
    }
}

private fun LazyListScope.profileTabEmpty(
    tab: ProfileTab,
    title: String,
    description: String,
) {
    item(key = "profile-empty-${tab.name}") {
        ProfileEmptyState(title = title, description = description)
    }
}

@Preview
@Composable
private fun SignedOutProfilePreview() {
    notmidTheme {
        ProfileContent(
            state = ProfileUiState(NotmidAuthState(NotmidAuthMode.Disabled, null, emptyList())),
            listState = rememberLazyListState(),
        ) {}
    }
}
