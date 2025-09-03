@file:OptIn(ExperimentalMaterialApi::class)

package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed interface NavigationButton {
    val contentDescription: Int

    sealed interface Simple : NavigationButton {
        val image: ImageVector
    }

    sealed interface Animated : NavigationButton {
        @get:Composable
        val image: AnimatedImageVector
        val atEnd: Boolean
    }

    object Back : Simple {
        override val image get() = Icons.AutoMirrored.Filled.ArrowBack
        override val contentDescription get() = LR.string.back
    }

    object Close : Simple {
        override val image get() = Icons.Default.Close
        override val contentDescription get() = LR.string.close
    }

    data class CloseBack(val isClose: Boolean) : Animated {
        @get:Composable
        override val image get() = AnimatedImageVector.animatedVectorResource(IR.drawable.ic_anim_close_back)
        override val atEnd get() = !isClose
        override val contentDescription get() = if (atEnd) LR.string.back else LR.string.close
    }
}

object ThemedTopAppBar {
    sealed interface Style {
        data object Solid : Style
        data object Immersive : Style
    }
}

@Composable
fun ThemedTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    navigationButton: NavigationButton? = NavigationButton.Back,
    onNavigationClick: (() -> Unit)? = null,
    style: ThemedTopAppBar.Style = ThemedTopAppBar.Style.Solid,
    iconColor: Color = when (style) {
        ThemedTopAppBar.Style.Solid -> MaterialTheme.theme.colors.secondaryIcon01
        ThemedTopAppBar.Style.Immersive -> MaterialTheme.theme.colors.primaryIcon01
    },
    backgroundColor: Color = when (style) {
        ThemedTopAppBar.Style.Solid -> MaterialTheme.theme.colors.secondaryUi01
        ThemedTopAppBar.Style.Immersive -> MaterialTheme.theme.colors.primaryUi01
    },
    bottomShadow: Boolean = false,
    windowInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    actions: @Composable RowScope.(Color) -> Unit = {},
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = iconColor),
    ) {
        TopAppBar(
            navigationIcon = if (navigationButton != null) {
                {
                    NavigationIconButton(
                        onClick = onNavigationClick ?: {},
                        navigationButton = navigationButton,
                        tint = iconColor,
                    )
                }
            } else {
                null
            },
            title = title,
            actions = { actions(iconColor) },
            backgroundColor = backgroundColor,
            elevation = 0.dp,
            windowInsets = windowInsets,
            modifier = if (bottomShadow) {
                modifier
                    .zIndex(1f)
                    .shadow(4.dp)
            } else {
                modifier
            },
        )
    }
}

@Composable
fun ThemedTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationButton: NavigationButton? = NavigationButton.Back,
    onNavigationClick: (() -> Unit)? = null,
    style: ThemedTopAppBar.Style = ThemedTopAppBar.Style.Solid,
    iconColor: Color = when (style) {
        ThemedTopAppBar.Style.Solid -> MaterialTheme.theme.colors.secondaryIcon01
        ThemedTopAppBar.Style.Immersive -> MaterialTheme.theme.colors.primaryIcon01
    },
    textColor: Color = when (style) {
        ThemedTopAppBar.Style.Solid -> MaterialTheme.theme.colors.secondaryText01
        ThemedTopAppBar.Style.Immersive -> MaterialTheme.theme.colors.primaryText01
    },
    backgroundColor: Color = when (style) {
        ThemedTopAppBar.Style.Solid -> MaterialTheme.theme.colors.secondaryUi01
        ThemedTopAppBar.Style.Immersive -> MaterialTheme.theme.colors.primaryUi01
    },
    bottomShadow: Boolean = false,
    titleOverflow: TextOverflow = TextOverflow.Ellipsis,
    windowInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    actions: @Composable RowScope.(Color) -> Unit = {},
) {
    ThemedTopAppBar(
        title = {
            if (title != null) {
                Text(
                    text = title,
                    color = textColor,
                    overflow = titleOverflow,
                )
            }
        },
        navigationButton = navigationButton,
        onNavigationClick = onNavigationClick,
        style = style,
        iconColor = iconColor,
        backgroundColor = backgroundColor,
        bottomShadow = bottomShadow,
        windowInsets = windowInsets,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
fun NavigationIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationButton: NavigationButton = NavigationButton.Back,
    tint: Color = MaterialTheme.theme.colors.secondaryIcon01,
) {
    val painter = when (navigationButton) {
        is NavigationButton.Simple -> rememberVectorPainter(navigationButton.image)
        is NavigationButton.Animated -> rememberAnimatedVectorPainter(navigationButton.image, navigationButton.atEnd)
    }

    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter,
            stringResource(navigationButton.contentDescription),
            tint = tint,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemedTopAppBarPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        var isClose by remember { mutableStateOf(true) }
        Column {
            ThemedTopAppBar(
                title = "No button",
                navigationButton = null,
            )
            ThemedTopAppBar(
                title = "Back button",
                navigationButton = NavigationButton.Back,
            )
            ThemedTopAppBar(
                title = "Close button",
                navigationButton = NavigationButton.Close,
            )
            ThemedTopAppBar(
                title = "Close / Back button",
                navigationButton = NavigationButton.CloseBack(isClose = isClose),
                onNavigationClick = { isClose = !isClose },
            )
            ThemedTopAppBar(
                title = "Immersive theme",
                navigationButton = NavigationButton.Back,
                style = ThemedTopAppBar.Style.Immersive,
            )
        }
    }
}
