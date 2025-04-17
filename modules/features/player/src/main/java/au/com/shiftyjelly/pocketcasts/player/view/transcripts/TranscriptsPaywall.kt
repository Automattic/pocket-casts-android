package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun TranscriptsPaywall(
    modifier: Modifier = Modifier,
    onClickSubscribe: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = null, onClick = {})
            .then(modifier)
            .padding(top = 24.dp, end = 16.dp, start = 16.dp),
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
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH50(
            text = stringResource(LR.string.transcript_generated_paywall_description),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(color = Color.Black),
        ) {
            RowButton(
                text = stringResource(LR.string.onboarding_subscribe_to_plus),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.plusGoldLight,
                ),
                onClick = onClickSubscribe,
            )
        }
    }
}

@Preview
@Composable
private fun TranscriptsPaywallPreview() {
    AppTheme(
        themeType = Theme.ThemeType.DARK,
    ) {
        TranscriptsPaywall(
            modifier = Modifier.background(Color(0xFF00082E)),
            onClickSubscribe = {},
        )
    }
}
