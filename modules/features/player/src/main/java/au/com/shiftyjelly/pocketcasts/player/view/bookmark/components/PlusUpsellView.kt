package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlusUpsellView(
    style: MessageViewColors,
) {
    MessageView(
        titleView = {
            Image(
                painter = painterResource(IR.drawable.pocket_casts_plus_logo),
                contentDescription = stringResource(LR.string.pocket_casts),
            )
        },
        buttonTitleRes = LR.string.subscribe, // TODO: Bookmarks update upsell button title based on subscription status
        buttonAction = { /* TODO */ },
        style = style
    )
}

@Preview
@Composable
private fun PlusUpsellPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PlusUpsellView(MessageViewColors.Default)
    }
}
