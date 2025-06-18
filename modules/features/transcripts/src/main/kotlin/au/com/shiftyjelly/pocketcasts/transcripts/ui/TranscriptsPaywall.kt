package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun TranscriptsPaywall(
    isFreeTrialAvailable: Boolean,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    theme: TranscriptTheme = TranscriptTheme.default(MaterialTheme.theme.colors),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = null, onClick = {})
            .verticalScroll(rememberScrollState())
            .then(modifier),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background)
                .padding(top = 24.dp),
        ) {
            SubscriptionBadge(
                iconRes = R.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                fontSize = 16.sp,
                padding = 6.dp,
                textColor = Color.Black,
                iconColor = Color.Black,
                backgroundBrush = Brush.horizontalGradient(
                    0f to Color.plusGoldLight,
                    1f to Color.plusGoldDark,
                ),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            TextH20(
                text = stringResource(LR.string.transcript_generated_paywall_title),
                color = theme.primaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            TextH50(
                text = stringResource(LR.string.transcript_generated_paywall_description),
                color = theme.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        val gradientBrush = remember(theme.background) {
            val colorsStops = arrayOf(
                0.0f to theme.background,
                0.05f to theme.background,
                0.15f to Color.Transparent,
                1f to Color.Transparent,
            )
            Brush.verticalGradient(colorStops = colorsStops)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(gradientBrush),
        ) {
            Spacer(
                modifier = Modifier.weight(1f),
            )
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
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TranscriptsPaywallPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        TranscriptsPaywall(
            isFreeTrialAvailable = false,
            onClickSubscribe = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview
@Composable
private fun TranscriptsPaywallPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val transcriptTheme = rememberTranscriptTheme()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(transcriptTheme.background)
                    .padding(horizontal = 16.dp),
            ) {
                TranscriptsPaywall(
                    isFreeTrialAvailable = true,
                    theme = transcriptTheme,
                    onClickSubscribe = {},
                )
            }
        }
    }
}
