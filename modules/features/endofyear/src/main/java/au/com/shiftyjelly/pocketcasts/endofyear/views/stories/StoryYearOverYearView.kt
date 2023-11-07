package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.endofyear.components.GradientPillar
import au.com.shiftyjelly.pocketcasts.endofyear.components.PillarStyle
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryFontFamily
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.disableScale
import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryYearOverYear
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val YearFontSize = 50
private const val MinimumPillarPercentage = 0.4

@Composable
fun StoryYearOverYearView(
    story: StoryYearOverYear,
    modifier: Modifier = Modifier,
    userTier: UserTier,
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 30.dp)
    ) {
        Spacer(modifier = modifier.height(40.dp))

        SubscriptionBadgeForTier(
            tier = SubscriptionTier.fromUserTier(userTier),
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )

        Spacer(modifier = modifier.height(14.dp))

        PrimaryText(story)

        Spacer(modifier = modifier.height(14.dp))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.2f))

        YearPillars(
            story = story,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

@Composable
private fun PrimaryText(
    story: StoryYearOverYear,
    modifier: Modifier = Modifier,
) {
    val listeningPercentage = story.yearOverYearListeningTime.percentage
    val text = when {
        listeningPercentage == Double.POSITIVE_INFINITY -> stringResource(
            LR.string.eoy_year_over_year_title_skyrocketed,
        )

        listeningPercentage > 10 -> stringResource(
            LR.string.end_of_year_stories_year_over_year_title_went_up,
            story.yearOverYearListeningTime.formattedPercentage
        )

        listeningPercentage < 0 -> stringResource(
            LR.string.end_of_year_stories_year_over_year_title_went_down,
            story.yearOverYearListeningTime.formattedPercentage
        )

        else -> stringResource(
            LR.string.end_of_year_stories_year_over_year_title_flat,
        )
    }
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryYearOverYear,
    modifier: Modifier = Modifier,
) {
    val listeningPercentage = story.yearOverYearListeningTime.percentage
    val text = when {
        listeningPercentage > 10 -> stringResource(
            LR.string.end_of_year_stories_year_over_year_subtitle_went_up,
            story.yearOverYearListeningTime.formattedPercentage
        )

        listeningPercentage < 0 -> stringResource(
            LR.string.end_of_year_stories_year_over_year_subtitle_went_down,
            story.yearOverYearListeningTime.formattedPercentage
        )

        else -> stringResource(
            LR.string.end_of_year_stories_year_over_year_subtitle_flat,
        )
    }
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Composable
fun YearPillars(
    story: StoryYearOverYear,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.Bottom,
        ) {
            GradientPillar(
                pillarStyle = PillarStyle.Grey,
                modifier = Modifier
                    .height((this@BoxWithConstraints.maxHeight.value * previousYearPillarPercentageSize(story.yearOverYearListeningTime)).dp)
                    .weight(0.5f)
            ) {
                YearTextContent(
                    year = "2022",
                    textColor = story.subtitleColor,
                    playedTime = story.yearOverYearListeningTime.totalPlayedTimeLastYear,
                )
            }

            GradientPillar(
                pillarStyle = PillarStyle.Rainbow,
                modifier = Modifier
                    .height((this@BoxWithConstraints.maxHeight.value * currentYearPillarPercentageSize(story.yearOverYearListeningTime)).dp)
                    .weight(0.5f),
            ) {
                YearTextContent(
                    year = "2023",
                    textColor = story.tintColor,
                    playedTime = story.yearOverYearListeningTime.totalPlayedTimeThisYear,
                )
            }
        }
    }
}

@Composable
private fun YearTextContent(
    year: String,
    textColor: Color,
    playedTime: Long,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(playedTime, context.resources)
    Column(
        verticalArrangement = Arrangement.Top,
    ) {
        AutoResizeText(
            text = year,
            color = textColor,
            lineHeight = YearFontSize.sp,
            fontFamily = StoryFontFamily,
            fontWeight = FontWeight.W500,
            maxFontSize = YearFontSize.sp,
            maxLines = 1,
        )
        TextH50(
            text = timeText,
            color = textColor,
            disableScale = disableScale(),
            fontFamily = StoryFontFamily,
            fontWeight = FontWeight.W600,
        )
    }
}

private fun previousYearPillarPercentageSize(
    yearOverYearListeningTime: YearOverYearListeningTime,
) = when {
    yearOverYearListeningTime.percentage == Double.POSITIVE_INFINITY -> 0.25
    yearOverYearListeningTime.percentage > 0.0 -> max(
        yearOverYearListeningTime.totalPlayedTimeLastYear.toDouble() / yearOverYearListeningTime.totalPlayedTimeThisYear,
        MinimumPillarPercentage
    )

    else -> 1.0
}

private fun currentYearPillarPercentageSize(
    yearOverYearListeningTime: YearOverYearListeningTime,
) = when {
    yearOverYearListeningTime.percentage < 0.0 -> max(
        yearOverYearListeningTime.totalPlayedTimeThisYear.toDouble() / yearOverYearListeningTime.totalPlayedTimeLastYear,
        MinimumPillarPercentage
    )

    else -> 1.0
}

@Preview(name = "Went down")
@Composable
fun YearOverYearWentDownPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryYearOverYearView(
            story = StoryYearOverYear(
                yearOverYearListeningTime = YearOverYearListeningTime(totalPlayedTimeThisYear = 200, totalPlayedTimeLastYear = 400)
            ),
            userTier = UserTier.Plus,
        )
    }
}

@Preview(name = "Went up")
@Composable
fun YearOverYearWentUpPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryYearOverYearView(
            story = StoryYearOverYear(
                YearOverYearListeningTime(totalPlayedTimeThisYear = 200, totalPlayedTimeLastYear = 130),
            ),
            userTier = UserTier.Plus,
        )
    }
}

@Preview(name = "Stayed same")
@Composable
fun YearOverYearStayedSamePreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryYearOverYearView(
            story = StoryYearOverYear(
                YearOverYearListeningTime(totalPlayedTimeThisYear = 140, totalPlayedTimeLastYear = 140),
            ),
            userTier = UserTier.Plus,
        )
    }
}

@Preview(name = "No listening time for past year")
@Composable
fun YearOverYearNoListeningTimePastYearPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        StoryYearOverYearView(
            story = StoryYearOverYear(
                YearOverYearListeningTime(totalPlayedTimeThisYear = 140, totalPlayedTimeLastYear = 0),
            ),
            userTier = UserTier.Plus,
        )
    }
}
