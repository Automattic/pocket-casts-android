package au.com.shiftyjelly.pocketcasts.compose.text

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.theme.colors.primaryText01,
    linkColor: Color = MaterialTheme.theme.colors.primaryInteractive01,
    maxLines: Int = Int.MAX_VALUE,
    @StyleRes textStyleResId: Int = UR.style.H50,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextAppearance(textStyleResId)
                setMaxLines(maxLines)
                setTextColor(color.toArgb())
                setLinkTextColor(linkColor.toArgb())
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) },
    )
}

@Preview(name = "Light")
@Composable
fun HtmlTextLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        HtmlText(
            html = "<a href=\"https://pocketcasts.com\">Pocket Casts</a> is a powerful podcast platform.",
        )
    }
}

@Preview(name = "Dark")
@Composable
fun HtmlTextDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        HtmlText(
            html = "<a href=\"https://pocketcasts.com\">Pocket Casts</a> is a powerful podcast platform.",
        )
    }
}
