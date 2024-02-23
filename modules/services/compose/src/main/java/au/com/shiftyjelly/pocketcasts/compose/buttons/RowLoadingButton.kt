package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

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
    onClick: () -> Unit,
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
                    strokeWidth = 2.dp,
                )
            }
        } else {
            null
        },
        onClick = onClick@{
            if (isLoading) return@onClick
            onClick()
        },
    )
}

@ShowkaseComposable(name = "RowLoadingButton", group = "Button", styleName = "Light", defaultStyle = true)
@Preview(name = "Light")
@Composable
fun RowLoadingButtonLightPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(
            text = "Accept",
            isLoading = true,
            onClick = {},
        )
    }
}

@ShowkaseComposable(name = "RowLoadingButton", group = "Button", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun RowLoadingButtonDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        RowLoadingButton(
            text = "Accept",
            isLoading = true,
            onClick = {},
        )
    }
}

@ShowkaseComposable(name = "RowLoadingButton", group = "Button", styleName = "Disabled")
@Preview(name = "Disabled")
@Composable
fun RowLoadingButtonDisabledPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(
            text = "Accept",
            enabled = false,
            isLoading = true,
            onClick = {},
        )
    }
}

@ShowkaseComposable(name = "RowLoadingButton", group = "Button", styleName = "No padding")
@Preview(name = "No padding")
@Composable
fun RowLoadingButtonNoPaddingPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(
            text = "Accept",
            includePadding = false,
            isLoading = true,
            onClick = {},
        )
    }
}

@ShowkaseComposable(name = "RowLoadingButton", group = "Button", styleName = "Text icon")
@Preview(name = "Text icon")
@Composable
fun RowLoadingButtonTextIconPreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        RowLoadingButton(
            text = "Share",
            isLoading = true,
            onClick = {},
        )
    }
}
