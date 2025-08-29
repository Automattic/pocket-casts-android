package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

/**
 * A styled App Bar that works in a Bottom Sheet component. It keeps the page background one color.
 */
@Composable
fun BottomSheetAppBar(
    title: String? = null,
    navigationButton: NavigationButton = NavigationButton.Back,
    windowInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Horizontal),
    actions: @Composable RowScope.(Color) -> Unit = {},
    onNavigationClick: () -> Unit,
) {
    ThemedTopAppBar(
        title = title,
        iconColor = MaterialTheme.theme.colors.primaryIcon01,
        textColor = MaterialTheme.theme.colors.primaryText01,
        backgroundColor = MaterialTheme.theme.colors.primaryUi01,
        windowInsets = windowInsets,
        navigationButton = navigationButton,
        actions = actions,
        onNavigationClick = onNavigationClick,
    )
}

@Preview(name = "Light")
@Composable
private fun BottomSheetAppBarLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
    }
}

@Preview(name = "Back")
@Composable
private fun BottomSheetAppBarBackPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Back, onNavigationClick = {})
    }
}

@Preview(name = "Dark")
@Composable
private fun BottomSheetAppBarDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
    }
}
