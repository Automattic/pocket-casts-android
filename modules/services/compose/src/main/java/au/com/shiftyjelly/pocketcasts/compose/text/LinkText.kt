package au.com.shiftyjelly.pocketcasts.compose.text

import androidx.compose.foundation.clickable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun LinkText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    onClick: () -> Unit
) {
    TextH40(
        text = text,
        textAlign = textAlign,
        color = MaterialTheme.theme.colors.primaryInteractive01,
        modifier = modifier.clickable { onClick() },
    )
}

@ShowkaseComposable(name = "LinkText", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun LinkTextLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        LinkText(text = "Hello World", onClick = {})
    }
}

@ShowkaseComposable(name = "LinkText", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun LinkTextDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        LinkText(text = "Hello World", onClick = {})
    }
}
