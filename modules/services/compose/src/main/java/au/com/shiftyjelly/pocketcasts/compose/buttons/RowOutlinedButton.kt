package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val outlinedBorder: BorderStroke
    @Composable
    get() = BorderStroke(2.dp, MaterialTheme.colors.primary)

@Composable
fun RowOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    border: BorderStroke? = outlinedBorder,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    textIcon: Painter? = null,
    fontFamily: FontFamily? = null,
    fontSize: TextUnit? = null,
    leadingIcon: Painter? = null,
    tintIcon: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .then(if (includePadding) Modifier.padding(16.dp) else Modifier)
            .fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = border,
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                RowOutlinedImage(
                    image = leadingIcon,
                    colors = colors,
                    tintIcon = tintIcon
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RowOutlinedImage(
                        image = textIcon,
                        colors = colors,
                        tintIcon = tintIcon
                    )
                    TextH30(
                        text = text,
                        color = colors.contentColor(enabled = true).value,
                        textAlign = TextAlign.Center,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowOutlinedImage(image: Painter?, colors: ButtonColors, tintIcon: Boolean, modifier: Modifier = Modifier) {
    image ?: return
    Image(
        painter = image,
        contentDescription = null,
        colorFilter = if (tintIcon) ColorFilter.tint(colors.contentColor(enabled = true).value) else null,
        modifier = modifier
    )
}

@ShowkaseComposable(name = "RowOutlinedButton", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun RowOutlinedButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowOutlinedButton(
            text = "Share",
            textIcon = rememberVectorPainter(Icons.Default.Share),
            onClick = {}
        )
    }
}

@ShowkaseComposable(name = "RowOutlinedButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowOutlinedButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        RowOutlinedButton(
            text = "Share",
            textIcon = rememberVectorPainter(Icons.Default.Share),
            onClick = {}
        )
    }
}

@ShowkaseComposable(name = "RowOutlinedButton", group = "Button", styleName = "Leading icon")
@Preview(name = "Leading icon")
@Composable
fun RowOutlinedButtonLeadingIconPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowOutlinedButton(
            text = stringResource(LR.string.onboarding_continue_with_google),
            leadingIcon = painterResource(R.drawable.google_g),
            tintIcon = false,
            onClick = {}
        )
    }
}
