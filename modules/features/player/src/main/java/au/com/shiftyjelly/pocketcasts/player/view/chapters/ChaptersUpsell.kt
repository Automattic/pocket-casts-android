package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalRippleConfiguration
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
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersUpsell(
    isFreeTrialAvailable: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chaptersTheme = LocalChaptersTheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(chaptersTheme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 480.dp),
        ) {
            SubscriptionBadge(
                iconRes = R.drawable.ic_plus,
                shortNameRes = LR.string.pocket_casts_plus_short,
                contentDescriptionRes = LR.string.pocket_casts_plus_badge,
                fontSize = 14.sp,
                padding = 4.dp,
                textColor = Color.Black,
                iconColor = Color.Black,
                backgroundBrush = Brush.horizontalGradient(
                    0f to Color.plusGoldLight,
                    1f to Color.plusGoldDark,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextH30(
                text = stringResource(LR.string.chapters_generated_upsell_title),
                color = chaptersTheme.headerToggle,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextP40(
                text = stringResource(LR.string.chapters_generated_upsell_message),
                color = chaptersTheme.headerTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
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
                    fontWeight = FontWeight.SemiBold,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.plusGoldLight),
                    includePadding = false,
                    onClick = onUpgradeClick,
                )
            }
        }
    }
}

@Preview(name = "Light")
@Composable
private fun ChaptersUpsellLightPreview() = ChaptersUpsellPreview(Theme.ThemeType.LIGHT, isFreeTrialAvailable = true)

@Preview(name = "Dark")
@Composable
private fun ChaptersUpsellDarkPreview() = ChaptersUpsellPreview(Theme.ThemeType.DARK, isFreeTrialAvailable = false)

@Composable
private fun ChaptersUpsellPreview(theme: Theme.ThemeType, isFreeTrialAvailable: Boolean) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            ChaptersUpsell(
                isFreeTrialAvailable = isFreeTrialAvailable,
                onUpgradeClick = {},
            )
        }
    }
}
