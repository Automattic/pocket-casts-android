package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun UpsellView(
    style: MessageViewColors,
    activeTheme: Theme.ThemeType,
    onClick: () -> Unit,
    sourceView: SourceView,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UpsellViewModel>()
    Content(
        style = style,
        activeTheme = activeTheme,
        onClick = {
            viewModel.onClick(sourceView)
            onClick()
        },
        modifier = modifier,
    )
}

@Composable
private fun Content(
    style: MessageViewColors,
    activeTheme: Theme.ThemeType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoColor = if (activeTheme.darkTheme) Color.White else Color.Black
    val description = stringResource(LR.string.pocket_casts_patron)
    MessageView(
        titleView = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                Image(
                    painter = painterResource(IR.drawable.logo_pocket_casts),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(logoColor),
                )
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
        Content(
            style = MessageViewColors.Default,
            activeTheme = themeType,
            onClick = {},
        )
    }
}
