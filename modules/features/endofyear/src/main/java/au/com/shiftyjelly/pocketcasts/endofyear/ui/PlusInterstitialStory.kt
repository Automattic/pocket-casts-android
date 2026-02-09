package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.ui.components.VideoSurface
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
@OptIn(UnstableApi::class)
internal fun PlusInterstitialStory(
    story: Story.PlusInterstitial,
    measurements: EndOfYearMeasurements,
    onClickUpsell: () -> Unit,
    onClickContinue: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        // as it's impossible to match the video background colour, use another video which can be stretched for the rest of the background
        VideoSurface(
            videoResourceId = R.raw.playback_plus_interstitial_background,
            backgroundColor = story.backgroundColor,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = measurements.closeButtonBottomEdge),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f),
            ) {
                VideoSurface(
                    videoResourceId = R.raw.playback_plus_interstitial,
                    videoRatio = 980f / 1338f,
                    backgroundColor = story.backgroundColor,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxSize(),
                )
            }
            PlusInfo(
                story = story,
                measurements = measurements,
                onClickUpsell = onClickUpsell,
                onClickContinue = onClickContinue,
            )
        }
    }
}

@Composable
private fun PlusInfo(
    story: Story.PlusInterstitial,
    measurements: EndOfYearMeasurements,
    onClickUpsell: () -> Unit,
    onClickContinue: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        ) {
            SubscriptionBadgeForTier(
                tier = story.subscriptionTier ?: SubscriptionTier.Plus,
                displayMode = SubscriptionBadgeDisplayMode.Black,
            )

            Spacer(Modifier.height(16.dp))

            val title = if (story.subscriptionTier == null) {
                stringResource(LR.string.end_of_year_stories_theres_more)
            } else {
                stringResource(LR.string.end_of_year_stories_plus_title)
            }
            TextH10(
                text = title,
                fontScale = measurements.smallDeviceFactor,
                disableAutoScale = true,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(16.dp))

            val description = if (story.subscriptionTier == null) {
                stringResource(LR.string.end_of_year_stories_subscribe_to_plus)
            } else {
                stringResource(LR.string.end_of_year_stories_already_subscribed_to_plus)
            }
            TextP40(
                text = description,
                lineHeight = 20.85.sp,
                fontWeight = FontWeight.W500,
                disableAutoScale = true,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        val buttonLabel = if (story.subscriptionTier == null) {
            stringResource(LR.string.eoy_story_stories_subscribe_to_plus_button_label)
        } else {
            stringResource(LR.string.navigation_continue)
        }
        SolidEoyButton(
            text = buttonLabel,
            backgroundColor = colorResource(UR.color.coolgrey_90),
            textColor = Color.White,
            onClick = {
                if (story.subscriptionTier == null) {
                    onClickUpsell()
                } else {
                    onClickContinue()
                }
            },
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PlusInterstitialPreview() {
    PreviewBox(currentPage = 7, progress = 1f) { measurements ->
        PlusInterstitialStory(
            story = Story.PlusInterstitial(subscriptionTier = SubscriptionTier.Plus),
            measurements = measurements,
            onClickUpsell = {},
            onClickContinue = {},
        )
    }
}
