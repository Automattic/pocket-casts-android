package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
    object Back : NavigationButton(image = Icons.Default.ArrowBack, contentDescription = LR.string.back)
    object Close : NavigationButton(image = Icons.Default.Close, contentDescription = LR.string.close)
}

@Composable
fun ThemedTopAppBar(
    title: String? = null,
    navigationButton: NavigationButton = NavigationButton.Back,
    iconColor: Color = MaterialTheme.theme.colors.secondaryIcon01,
    textColor: Color = MaterialTheme.theme.colors.secondaryText01,
    backgroundColor: Color = MaterialTheme.theme.colors.secondaryUi01,
    bottomShadow: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    onNavigationClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    onNavigationClick()
                }
            ) {
                Icon(
                    navigationButton.image,
                    stringResource(navigationButton.contentDescription),
                    tint = iconColor
                )
            }
        },
        title = {
            if (title != null) {
                Text(
                    text = title,
                    color = textColor
                )
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        modifier = if (bottomShadow) Modifier.zIndex(1f).shadow(4.dp) else Modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ThemedTopAppBarPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Column {
            ThemedTopAppBar(title = "Hello World", navigationButton = NavigationButton.Back, onNavigationClick = {})
            ThemedTopAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
        }
    }
}
