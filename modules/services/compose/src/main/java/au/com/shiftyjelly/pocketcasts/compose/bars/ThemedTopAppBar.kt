@file:OptIn(ExperimentalMaterialApi::class)

package au.com.shiftyjelly.pocketcasts.compose.bars

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class NavigationButton(val image: ImageVector, val contentDescription: Int) {
    object Back : NavigationButton(image = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LR.string.back)
    object Close : NavigationButton(image = Icons.Default.Close, contentDescription = LR.string.close)
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
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = iconColor),
    ) {
        TopAppBar(
            navigationIcon = if (navigationButton != null) {
                {
                    NavigationIconButton(
                        onNavigationClick = onNavigationClick ?: {},
                        navigationButton = navigationButton,
                        iconColor = iconColor,
                    )
                }
            } else {
                null
            },
            title = {
                if (title != null) {
                    Text(
                        text = title,
                        color = textColor,
                        overflow = titleOverflow,
                    )
                }
            },
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
fun NavigationIconButton(
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationButton: NavigationButton = NavigationButton.Back,
    iconColor: Color = MaterialTheme.theme.colors.secondaryIcon01,
) {
    IconButton(
        onClick = onNavigationClick,
        modifier = modifier,
    ) {
        Icon(
            navigationButton.image,
            stringResource(navigationButton.contentDescription),
            tint = iconColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemedTopAppBarPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Column {
            ThemedTopAppBar(title = "Hello World", navigationButton = null)
            ThemedTopAppBar(title = "Hello World", navigationButton = NavigationButton.Back, onNavigationClick = {})
            ThemedTopAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
            ThemedTopAppBar(title = "Hello World", navigationButton = NavigationButton.Back, style = ThemedTopAppBar.Style.Immersive, onNavigationClick = {})
        }
    }
}
