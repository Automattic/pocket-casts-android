package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun RowTextButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    textIcon: Painter? = null,
    fontSize: TextUnit? = null,
    tintIcon: Boolean = true,
    onClick: () -> Unit,
    fullWidth: Boolean = true,
) {
    RowOutlinedButton(
        text = text,
        modifier = modifier,
        includePadding = includePadding,
        border = null,
        colors = colors,
        textIcon = textIcon,
        fontSize = fontSize,
        tintIcon = tintIcon,
        onClick = onClick,
        fullWidth = fullWidth,
    )
}

@ShowkaseComposable(name = "RowTextButton", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun RowTextButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowTextButton(text = "Log in", onClick = {})
    }
}

@ShowkaseComposable(name = "RowTextButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowTextButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowTextButton(text = "Log in", onClick = {})
    }
}
