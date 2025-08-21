package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun RowTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    textIcon: Painter? = null,
    fontSize: TextUnit? = null,
    tintIcon: Boolean = true,
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

@Preview(name = "Light")
@Composable
private fun RowTextButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowTextButton(text = "Log in", onClick = {})
    }
}

@Preview(name = "Dark")
@Composable
private fun RowTextButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowTextButton(text = "Log in", onClick = {})
    }
}
