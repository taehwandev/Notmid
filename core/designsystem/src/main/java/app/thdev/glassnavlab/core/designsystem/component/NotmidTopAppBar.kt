package app.thdev.glassnavlab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.thdev.glassnavlab.core.designsystem.theme.NotmidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotmidTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = NotmidTheme.colors.background,
) {
    TopAppBar(
        title = {
            NotmidTopAppBarTitle(
                title = title,
                subtitle = subtitle,
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = NotmidTheme.colors.glassLightStrong,
            titleContentColor = NotmidTheme.colors.content,
            navigationIconContentColor = NotmidTheme.colors.content,
            actionIconContentColor = NotmidTheme.colors.content,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotmidCenterAlignedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = NotmidTheme.colors.background,
) {
    CenterAlignedTopAppBar(
        title = {
            NotmidTopAppBarTitle(
                title = title,
                subtitle = subtitle,
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = NotmidTheme.colors.glassLightStrong,
            titleContentColor = NotmidTheme.colors.content,
            navigationIconContentColor = NotmidTheme.colors.content,
            actionIconContentColor = NotmidTheme.colors.content,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotmidLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = NotmidTheme.colors.background,
) {
    LargeTopAppBar(
        title = {
            NotmidTopAppBarTitle(
                title = title,
                subtitle = subtitle,
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = NotmidTheme.colors.glassLightStrong,
            titleContentColor = NotmidTheme.colors.content,
            navigationIconContentColor = NotmidTheme.colors.content,
            actionIconContentColor = NotmidTheme.colors.content,
        ),
    )
}

@Composable
private fun NotmidTopAppBarTitle(
    title: String,
    subtitle: String?,
) {
    if (subtitle == null) {
        NotmidText(
            text = title,
            variant = NotmidTextVariant.Title,
            maxLines = 1,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(NotmidTheme.spacing.xxs)) {
            NotmidText(
                text = title,
                variant = NotmidTextVariant.Title,
                maxLines = 1,
            )
            NotmidText(
                text = subtitle,
                variant = NotmidTextVariant.Caption,
                color = NotmidTheme.colors.contentMuted,
                maxLines = 1,
            )
        }
    }
}
