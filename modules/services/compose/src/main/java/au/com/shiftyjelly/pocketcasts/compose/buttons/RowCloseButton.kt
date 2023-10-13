package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
fun RowCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = onClose
        ) {
            Icon(
                imageVector = NavigationButton.Close.image,
                contentDescription = stringResource(NavigationButton.Close.contentDescription),
                tint = tintColor,
            )
        }
    }
}

@ShowkaseComposable(name = "RowCloseButton", group = "Button", styleName = "Light")
@Preview(name = "Light")
@Composable
fun RowCloseButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        RowCloseButton(
            onClose = {},
        )
    }
}

@ShowkaseComposable(name = "RowCloseButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowCloseButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowCloseButton(
            onClose = {},
            tintColor = Color.Black,
        )
    }
}
