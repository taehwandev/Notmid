package app.thdev.glassnavlab.feature.profile

import androidx.compose.runtime.Immutable
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidClip
import app.thdev.glassnavlab.feature.notmid.common.model.NotmidDestination

@Immutable
internal data class ProfileUiState(
    val auth: NotmidAuthState,
    val destination: NotmidDestination? = null,
    val selectedTab: ProfileTab = ProfileTab.Clips,
    val ownClips: List<NotmidClip> = emptyList(),
)
