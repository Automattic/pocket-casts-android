package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun RowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    fontFamily: FontFamily? = null,
    @DrawableRes leadingIcon: Int? = null,
    onClick: () -> Unit,
    cornerRadius: Dp = 12.dp,
    @DrawableRes textIcon: Int? = null,
) {
    Box(
        modifier = modifier
            .then(if (includePadding) Modifier.padding(16.dp) else Modifier)
            .fillMaxWidth()
    ) {
        Button(
            onClick = { onClick() },
            shape = RoundedCornerShape(cornerRadius),
            border = border,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            enabled = enabled
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (leadingIcon != null) {
                    Image(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(4.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (textIcon != null) {
                        Image(
                            painter = painterResource(textIcon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(textColor),
                            modifier = Modifier
                        )
                    }
                    TextP40(
                        text = text,
                        fontFamily = fontFamily,
                        modifier = Modifier
                            .padding(6.dp),
                        textAlign = TextAlign.Center,
                        color = textColor
                    )
                }
            }
        }
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun RowButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowButton(text = "Accept", onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        RowButton(text = "Accept", onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Disabled")
@Preview(name = "Disabled")
@Composable
fun RowButtonDisabledPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowButton(text = "Accept", enabled = false, onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "No padding")
@Preview(name = "No padding")
@Composable
fun RowButtonNoPaddingPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowButton(text = "Accept", includePadding = false, onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Text icon")
@Preview(name = "Text icon")
@Composable
fun RowButtonTextIconPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowButton(
            text = "Share",
            textIcon = IR.drawable.ic_retry,
            onClick = {}
        )
    }
}
