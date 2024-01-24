package au.com.shiftyjelly.pocketcasts.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ProductAmountVerticalText(
    primaryText: String,
    secondaryText: String? = null,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    emphasized: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        if (emphasized) {
            TextH30(
                text = primaryText,
                color = MaterialTheme.theme.colors.primaryText01,
            )
        } else {
            TextH40(
                text = primaryText,
                color = MaterialTheme.theme.colors.primaryText01,
            )
        }
        if (secondaryText != null) {
            TextP60(
                text = secondaryText,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Composable
fun ProductAmountHorizontalText(
    primaryText: String? = null,
    secondaryText: String? = null,
    lineThroughSecondaryText: Boolean = true,
    hasBackgroundAlwaysWhite: Boolean = false,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
    ) {
        if (primaryText != null) {
            TextH30(
                text = primaryText,
                color =
                if (hasBackgroundAlwaysWhite) {
                    Color.Black
                } else {
                    MaterialTheme.theme.colors.primaryText01
                },
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (secondaryText != null) {
            TextP60(
                text = secondaryText,
                color = MaterialTheme.theme.colors.primaryText02,
                style = TextStyle(
                    textDecoration = if (lineThroughSecondaryText) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductAmountPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ProductAmountVerticalText(
            primaryText = "4 days free",
            secondaryText = "then $0.99 / month",
        )
    }
}
