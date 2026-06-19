package au.com.shiftyjelly.pocketcasts.compose.summary

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.text.markdownToHtml
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

data class SummaryPaywallColors(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
) {
    companion object {
        @Composable
        fun default() = SummaryPaywallColors(
            background = MaterialTheme.theme.colors.primaryUi01,
            primaryText = MaterialTheme.theme.colors.primaryText01,
            secondaryText = MaterialTheme.theme.colors.primaryText02,
        )

        fun player(
            background: Color,
            contrast01: Color,
            contrast02: Color,
        ) = SummaryPaywallColors(
            background = background,
            primaryText = contrast01,
            secondaryText = contrast02,
        )
    }
}

@Composable
fun SummaryPaywall(
    summaryText: String,
    isFreeTrialAvailable: Boolean,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SummaryPaywallColors = SummaryPaywallColors.default(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val startPadding = contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    val endPadding = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()

    val obscureModifier = if (Build.VERSION.SDK_INT >= 31) {
        Modifier.blur(6.dp)
    } else {
        Modifier.alpha(0.1f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(colors.background)
            .padding(start = startPadding, end = endPadding),
    ) {
        Spacer(modifier = Modifier.height(topPadding))
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
        Spacer(modifier = Modifier.height(24.dp))
        TextH20(
            text = stringResource(LR.string.summary_upsell_title),
            color = colors.primaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextH50(
            text = stringResource(LR.string.summary_upsell_description),
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))

        BlurredSummaryPreview(
            summaryText = summaryText,
            textColor = colors.primaryText,
            backgroundColor = colors.background,
            modifier = obscureModifier.weight(1f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(color = Color.Black),
        ) {
            RowButton(
                text = if (isFreeTrialAvailable) {
                    stringResource(LR.string.profile_start_free_trial)
                } else {
                    stringResource(LR.string.onboarding_subscribe_to_plus)
                },
                textColor = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.plusGoldLight,
                ),
                includePadding = false,
                onClick = onClickSubscribe,
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        }
    }
}

@Composable
private fun ColumnScope.BlurredSummaryPreview(
    summaryText: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val gradientBrush = Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.7f to Color.Transparent,
        1.0f to backgroundColor,
    )

    val html = remember(summaryText) { markdownToHtml(summaryText) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        HtmlText(
            html = html,
            color = textColor,
            textStyleResId = UR.style.P40,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradientBrush),
        )
    }
}

@Preview
@Composable
private fun SummaryPaywallPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        SummaryPaywall(
            summaryText = "## Episode Highlights\n\n- First key point discussed in the episode\n- Second important topic covered by the hosts\n- **Notable quote** from the guest speaker\n\nThe hosts wrap up with their final thoughts on the matter.",
            isFreeTrialAvailable = false,
            onClickSubscribe = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun SummaryPaywallFreeTrialPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SummaryPaywall(
            summaryText = "## Episode Highlights\n\n- First key point discussed\n- Second important topic\n- **Notable quote** from the guest",
            isFreeTrialAvailable = true,
            onClickSubscribe = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
