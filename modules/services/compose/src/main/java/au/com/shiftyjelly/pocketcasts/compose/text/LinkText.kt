package au.com.shiftyjelly.pocketcasts.compose.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun LinkText(
    text: String,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextH40(
        text = text,
        textAlign = textAlign,
        color = MaterialTheme.theme.colors.primaryInteractive01,
        modifier = modifier.clickable { onClick() }.padding(8.dp),
    )
}

@Preview(showBackground = true)
@Composable
fun LinkTextLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        LinkText(
            text = "Hello World",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun LinkTextDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        LinkText(
            text = "Hello World",
            onClick = {}
        )
    }
}
