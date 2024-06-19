package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
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
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    @DrawableRes leadingIcon: Int? = null,
    onClick: () -> Unit,
    cornerRadius: Dp = 12.dp,
    textVerticalPadding: Dp = 6.dp,
    @DrawableRes textIcon: Int? = null,
) {
    BaseRowButton(
        text = text,
        modifier = modifier,
        includePadding = includePadding,
        enabled = enabled,
        border = border,
        colors = colors,
        textColor = textColor,
        elevation = elevation,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp),
                    tint = textColor,
                )
            }
        } else {
            null
        },
        onClick = onClick,
        cornerRadius = cornerRadius,
        textVerticalPadding = textVerticalPadding,
        textIcon = textIcon,
    )
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
            onClick = {},
        )
    }
}
