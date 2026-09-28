package app.thdev.glassnavlab.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.glassnavlab.core.designsystem.component.NotmidButton
import app.thdev.glassnavlab.core.designsystem.component.NotmidButtonVariant
import app.thdev.glassnavlab.core.designsystem.component.NotmidGlassSurface
import app.thdev.glassnavlab.core.designsystem.component.NotmidPillButton
import app.thdev.glassnavlab.core.designsystem.theme.NotmidColorTokens
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme
import app.thdev.glassnavlab.core.designsystem.component.NotmidText
import app.thdev.glassnavlab.core.designsystem.component.NotmidTextVariant
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthUser

@Composable
internal fun ProfileHeader(
    user: NotmidAuthUser?,
    onSettingsRequested: () -> Unit,
) {
    val displayName = user?.displayName ?: "Your profile"
    val handle = user?.handle
    val roles = user?.roles.orEmpty()

    NotmidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = NotmidTheme.shapes.sheet,
        backgroundColor = NotmidTheme.colors.glassLight.copy(alpha = 0.82f),
        contentPadding = PaddingValues(NotmidTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.md),
            ) {
                ProfileAvatar(initial = handle?.take(1)?.uppercase() ?: "?")
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xs),
                ) {
                    if (handle != null) {
                        NotmidText(
                            text = "@$handle",
                            color = NotmidTheme.colors.signal,
                            variant = NotmidTextVariant.Label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    NotmidText(
                        text = displayName,
                        variant = NotmidTextVariant.Title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NotmidText(
                        text = user?.homeNeighborhood?.takeIf(String::isNotBlank)
                            ?: if (user == null) "Sign in to view your activity." else "Neighborhood not set",
                        color = NotmidTheme.colors.contentMuted,
                        variant = NotmidTextVariant.BodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (roles.isNotEmpty()) {
                NotmidText(
                    text = roles.joinToString(" · "),
                    color = NotmidTheme.colors.contentMuted,
                    variant = NotmidTextVariant.Label,
                )
            }
            NotmidButton(
                text = "Settings",
                onClick = onSettingsRequested,
                variant = NotmidButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun ProfileAvatar(initial: String) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        NotmidColorTokens.WarmClip,
                        NotmidColorTokens.NightViolet,
                        NotmidColorTokens.RouteBlue,
                    ),
                ),
            )
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.24f)),
        )
        NotmidText(
            text = initial,
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            color = NotmidColorTokens.Cloud,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            variant = NotmidTextVariant.Title,
        )
    }
}

@Composable
internal fun ProfileTabs(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.sm),
    ) {
        ProfileTab.entries.forEach { tab ->
            NotmidPillButton(
                label = tab.label,
                modifier = Modifier.semantics { selected = tab == selectedTab },
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}
