package au.com.shiftyjelly.pocketcasts.compose.bars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

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

@ShowkaseComposable(name = "BottomSheetAppBar", group = "Bottom sheet", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun BottomSheetAppBarLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
    }
}

@ShowkaseComposable(name = "BottomSheetAppBar", group = "Bottom sheet", styleName = "Back")
@Preview(name = "Back")
@Composable
fun BottomSheetAppBarBackPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Back, onNavigationClick = {})
    }
}

@ShowkaseComposable(name = "BottomSheetAppBar", group = "Bottom sheet", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun BottomSheetAppBarDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        BottomSheetAppBar(title = "Hello World", navigationButton = NavigationButton.Close, onNavigationClick = {})
    }
}
