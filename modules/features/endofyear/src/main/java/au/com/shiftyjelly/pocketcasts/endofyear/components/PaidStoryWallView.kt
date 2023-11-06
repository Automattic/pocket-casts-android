package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.UpsellButtonTitle
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PaidStoryWallView(
    freeTrial: FreeTrial,
    onUpsellClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .fadeBackground()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp)
    ) {
        Spacer(modifier = modifier.weight(0.2f))

        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PLUS,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )

        Spacer(modifier = modifier.height(14.dp))

        PrimaryText()

        Spacer(modifier = modifier.height(14.dp))

        SecondaryText()

        Spacer(modifier = modifier.height(14.dp))

        UpsellButton(
            freeTrial,
            onUpsellClicked,
        )

        Spacer(modifier = modifier.weight(1f))
    }
}

@Composable
private fun PrimaryText(
    modifier: Modifier = Modifier,
) {
    val text = stringResource(LR.string.end_of_year_stories_theres_more)
    StoryPrimaryText(text = text, color = Story.TintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    modifier: Modifier = Modifier,
) {
    val text = stringResource(LR.string.end_of_year_stories_subscribe_to_plus)
    StorySecondaryText(text = text, color = Story.SubtitleColor, modifier = modifier)
}

@Composable
private fun UpsellButton(
    freeTrial: FreeTrial,
    onUpsellClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StoryButton(
        text = UpsellButtonTitle(
            tier = freeTrial.subscriptionTier,
            hasFreeTrial = freeTrial.exists
        ),
        onClick = onUpsellClicked,
        modifier = modifier
    )
}

private fun Modifier.fadeBackground() = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        onDrawWithContent {
            drawRect(
                brush = Brush.linearGradient(
                    0.00f to Color.Black,
                    1.00f to Color.Black.copy(alpha = 0f),
                    start = Offset(0.5f * size.width, 0.24f * size.height),
                    end = Offset(0.5f * size.width, 1.04f * size.height)
                ),
                blendMode = BlendMode.DstIn
            )
            drawContent()
        }
    }

@Preview("has free trial")
@Composable
fun PaidStoryWallViewFreeTrialPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        PaidStoryWallView(
            freeTrial = FreeTrial(
                subscriptionTier = SubscriptionTier.PLUS,
                exists = true,
            ),
            onUpsellClicked = {}
        )
    }
}

@Preview("does not have free trial")
@Composable
fun PaidStoryWallViewNoFreeTrialPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        PaidStoryWallView(
            freeTrial = FreeTrial(
                subscriptionTier = SubscriptionTier.PLUS,
                exists = false,
            ),
            onUpsellClicked = {}
        )
    }
}
