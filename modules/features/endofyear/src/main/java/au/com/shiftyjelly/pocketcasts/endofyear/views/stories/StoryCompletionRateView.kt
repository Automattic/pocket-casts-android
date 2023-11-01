package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.endofyear.components.CompletionRateCircle
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryCompletionRateView(
    story: StoryCompletionRate,
    userTier: UserTier,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 40.dp)
    ) {
        Spacer(modifier = modifier.weight(0.2f))

        SubscriptionBadgeForTier(
            tier = SubscriptionTier.fromUserTier(userTier),
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )

        Spacer(modifier = modifier.height(14.dp))

        PrimaryText(story)

        Spacer(modifier = modifier.height(14.dp))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.2f))

        CompletionRateCircle(
            percent = story.episodesStartedAndCompleted.percentage.toInt(),
            titleColor = story.tintColor,
            subTitleColor = story.subtitleColor,
            modifier = modifier
                .weight(1f)
        )

        Spacer(modifier = modifier.weight(0.2f))
    }
}

@Composable
private fun PrimaryText(
    story: StoryCompletionRate,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        LR.string.end_of_year_stories_year_completion_rate_title,
        story.episodesStartedAndCompleted.percentage.toInt(),
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryCompletionRate,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        LR.string.end_of_year_stories_year_completion_rate_subtitle,
        story.episodesStartedAndCompleted.started,
        story.episodesStartedAndCompleted.completed,
    )
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Preview
@Composable
fun StoryCompletionRatPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryCompletionRateView(
            StoryCompletionRate(
                EpisodesStartedAndCompleted(started = 100, completed = 30),
            ),
            userTier = UserTier.Plus,
        )
    }
}
