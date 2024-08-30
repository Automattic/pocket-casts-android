package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
    contentDescription: String = stringResource(NavigationButton.Close.contentDescription),
) = IconButton(
    onClick = onClick,
    modifier = modifier,
) {
    Icon(
        imageVector = NavigationButton.Close.image,
        contentDescription = contentDescription,
        tint = tintColor,
    )
}

@ShowkaseComposable(name = "CloseButton", group = "Button", styleName = "Light")
@Preview(name = "Light")
@Composable
fun CloseButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        CloseButton(
            onClick = {},
        )
    }
}

@ShowkaseComposable(name = "CloseButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun CloseButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        CloseButton(
            onClick = {},
            tintColor = Color.Black,
        )
    }
}
