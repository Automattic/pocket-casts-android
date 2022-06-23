package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

/**
 * A styled App Bar that works in a Bottom Sheet component. It keeps the page background one color.
 */
@Composable
fun BottomSheetAppBar(
    title: String? = null,
    navigationButton: NavigationButton = NavigationButton.Back,
    actions: @Composable RowScope.() -> Unit = {},
    onNavigationClick: () -> Unit
) {
    ThemedTopAppBar(
        title = title,
        iconColor = MaterialTheme.theme.colors.primaryIcon01,
        textColor = MaterialTheme.theme.colors.primaryText01,
        backgroundColor = MaterialTheme.theme.colors.primaryUi01,
        navigationButton = navigationButton,
        actions = actions,
        onNavigationClick = onNavigationClick
    )
}

@Preview(showBackground = true)
@Composable
private fun BottomSheetAppBarPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        Column {
            BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Back, onNavigationClick = {})
            BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
        }
    }
}
