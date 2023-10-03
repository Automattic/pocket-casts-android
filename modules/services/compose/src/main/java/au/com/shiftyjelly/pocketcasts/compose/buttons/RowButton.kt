package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun RowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    @DrawableRes leadingIcon: Int? = null,
    onClick: () -> Unit
) {

    BaseRowButton(
        text = text,
        modifier = modifier,
        includePadding = includePadding,
        enabled = enabled,
        border = border,
        colors = colors,
        textColor = textColor,
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
        } else null,
        onClick = onClick,
    )
}
@Composable
fun RowLoadingButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    onClick: () -> Unit
) {

    BaseRowButton(
        text = text,
        modifier = modifier,
        includePadding = includePadding,
        enabled = enabled,
        border = border,
        colors = colors,
        textColor = textColor,
        leadingIcon = if (isLoading) {
            {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            }
        } else null,
        onClick = onClick@{
            if (isLoading) return@onClick
            onClick()
        },
    )
}

@Composable
private fun BaseRowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .then(if (includePadding) Modifier.padding(16.dp) else Modifier)
            .fillMaxWidth()
    ) {
        Button(
            onClick = { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = border,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            enabled = enabled
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(4.dp)
                    ) {
                        leadingIcon()
                    }
                }
                TextP40(
                    text = text,
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = textColor
                )
            }
        }
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun RowButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(text = "Accept", onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        RowLoadingButton(text = "Accept", onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "Disabled")
@Preview(name = "Disabled")
@Composable
fun RowButtonDisabledPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(text = "Accept", enabled = false, onClick = {})
    }
}

@ShowkaseComposable(name = "RowButton", group = "Button", styleName = "No padding")
@Preview(name = "No padding")
@Composable
fun RowButtonNoPaddingPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(text = "Accept", includePadding = false, onClick = {})
    }
}
