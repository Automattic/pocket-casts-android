package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlusUpsellBanner(
    title: String,
    body: String,
    buttonText: String,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    colors: NoContentBannerColors = PlusUpsellBannerColors.default(),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
            NoContentBanner(
                title = title,
                body = body,
                iconResourceId = IR.drawable.ic_plus,
                colors = colors,
                primaryButtonText = buttonText,
                onPrimaryButtonClick = onClickSubscribe,
                header = { PlusBadge() },
            )
        }
    }
}

@Composable
private fun PlusBadge() {
    SubscriptionBadge(
        iconRes = IR.drawable.ic_plus,
        shortNameRes = LR.string.pocket_casts_plus_short,
        contentDescriptionRes = LR.string.pocket_casts_plus_badge,
        fontSize = 16.sp,
        padding = 6.dp,
        textColor = Color.Black,
        iconColor = Color.Black,
        backgroundBrush = Brush.horizontalGradient(
            0f to Color.plusGoldLight,
            1f to Color.plusGoldDark,
        ),
    )
}

object PlusUpsellBannerColors {
    @Composable
    fun default() = NoContentBannerColors(
        primaryText = MaterialTheme.theme.colors.primaryText01,
        secondaryText = MaterialTheme.theme.colors.primaryText02,
        button = Color.plusGoldLight,
        buttonText = Color.Black,
        buttonRipple = Color.Black,
        icon = Color.Unspecified,
    )

    fun forColors(
        primaryText: Color,
        secondaryText: Color,
    ) = NoContentBannerColors(
        primaryText = primaryText,
        secondaryText = secondaryText,
        button = Color.plusGoldLight,
        buttonText = Color.Black,
        buttonRipple = Color.Black,
        icon = Color.Unspecified,
    )
}

@Preview
@Composable
private fun PlusUpsellBannerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        PlusUpsellBanner(
            title = "AI-Powered Summaries",
            body = "Subscribe to Plus to get access to it and other Premium features like bookmarks and folders.",
            buttonText = "Start Free Trial",
            onClickSubscribe = {},
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.theme.colors.primaryUi01),
        )
    }
}
