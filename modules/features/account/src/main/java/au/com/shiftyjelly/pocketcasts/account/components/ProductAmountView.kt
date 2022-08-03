package au.com.shiftyjelly.pocketcasts.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.account.util.ProductAmount
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ProductAmountView(
    productAmount: ProductAmount,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    emphasized: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        if (emphasized) {
            TextH30(
                text = productAmount.primaryText,
                color = MaterialTheme.theme.colors.primaryText01
            )
        } else {
            TextH40(
                text = productAmount.primaryText,
                color = MaterialTheme.theme.colors.primaryText01
            )
        }
        if (productAmount.secondaryText != null) {
            TextP60(
                text = productAmount.secondaryText,
                color = MaterialTheme.theme.colors.primaryText02
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
        ProductAmountView(
            ProductAmount(
                primaryText = "4 days free",
                secondaryText = "then $0.99 / month"
            )
        )
    }
}
