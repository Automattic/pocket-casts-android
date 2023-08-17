package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UpsellView(
    style: MessageViewColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageView(
        titleView = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextH20(text = stringResource(LR.string.pocket_casts))
                Spacer(modifier = Modifier.width(8.dp))
                SubscriptionBadge(
                    iconRes = IR.drawable.ic_patron,
                    shortNameRes = LR.string.pocket_casts_patron_short,
                    iconColor = Color.White,
                    textColor = Color.White,
                    backgroundColor = colorResource(UR.color.patron_purple),
                )
            }
        },
        buttonTitleRes = LR.string.subscribe, // TODO: Bookmarks update upsell button title based on subscription status
        buttonAction = onClick,
        style = style,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun UpsellPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        UpsellView(MessageViewColors.Default, {})
    }
}
