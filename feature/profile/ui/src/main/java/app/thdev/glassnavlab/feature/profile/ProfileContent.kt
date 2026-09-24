package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.tooling.preview.Preview
import app.thdev.glassnavlab.core.designsystem.theme.notmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthMode
import app.thdev.glassnavlab.core.model.notmid.NotmidNavigationIcon
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.thdev.glassnavlab.core.designsystem.component.NotmidSectionHeader
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidClipCard
import app.thdev.glassnavlab.feature.notmid.common.components.NotmidPlaceCard
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidPlace

@Composable
internal fun ProfileContent(
    destination: NotmidDestination,
    authState: NotmidAuthState,
    listState: LazyListState,
    onAction: (ProfileAction) -> Unit,
) {
    val user = authState.session?.user

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = ProfileContentPadding,
        verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg),
    ) {
        item(key = "profile-header-${destination.id}") {
            ProfileHeader(
                user = user,
                destination = destination,
                authState = authState,
                onSettingsRequested = { onAction(ProfileAction.OpenSettings) },
            )
        }

        item(key = "profile-tabs-${destination.id}") {
            ProfileTabs()
        }

        profileClips(destination.clips)
        profilePlaces(destination.places)

        item(key = "profile-settings-entry-${destination.id}") {
            ProfileSettingsEntry(
                authState = authState,
                onSettingsRequested = { onAction(ProfileAction.OpenSettings) },
            )
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

@Preview
@Composable
private fun SignedOutProfilePreview() {
    notmidTheme {
        ProfileContent(
            NotmidDestination("profile", "Profile", "Local", NotmidNavigationIcon.Inbox, emptyList(), emptyList(), emptyList(), null),
            NotmidAuthState(NotmidAuthMode.Disabled, null, emptyList()),
            rememberLazyListState(),
        ) {}
    }
}

private fun LazyListScope.profileClips(clips: List<NotmidClip>) {
    item(key = "profile-clips-header") {
        NotmidSectionHeader(
            title = "Receipts",
            subtitle = "Recent clips that prove the profile has real place context.",
            eyebrow = "clips",
        )
    }

    items(
        items = clips.take(2),
        key = { clip -> "profile-clip-${clip.id}" },
    ) { clip ->
        NotmidClipCard(clip = clip)
    }
}

private fun LazyListScope.profilePlaces(places: List<NotmidPlace>) {
    item(key = "profile-places-header") {
        NotmidSectionHeader(
            title = "Saved places",
            subtitle = "Places ready for map routes or chat planning.",
            eyebrow = "saves",
        )
    }

    items(
        items = places.take(2),
        key = { place -> "profile-place-${place.id}" },
    ) { place ->
        NotmidPlaceCard(
            place = place,
            index = places.indexOf(place),
        )
    }
}
