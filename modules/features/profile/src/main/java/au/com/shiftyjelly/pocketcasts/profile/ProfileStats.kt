package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ProfileStats(
    state: ProfileStatsState,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Max),
    ) {
        StatsColumn(
            numberText = state.podcastsCount.toString(),
            labelText = pluralStringResource(LR.plurals.podcast, state.podcastsCount).uppercase(),
        )

        val resource = LocalContext.current.resources
        val (listenedCount, listenedText) = remember(state.listenedDuration) {
            val text = state.listenedDuration.toFriendlyString(
                resources = resource,
                maxPartCount = 1,
                pluralResourceId = { it.listenedPlural },
            )
            text.substringBefore('\u00a0') to text.substringAfter('\u00a0').uppercase()
        }
        StatsColumn(
            numberText = listenedCount,
            labelText = listenedText,
        )

        val (savedCount, savedText) = remember(state.savedDuration) {
            val text = state.savedDuration.toFriendlyString(
                resources = resource,
                maxPartCount = 1,
                pluralResourceId = { it.savedPlural },
            )
            text.substringBefore('\u00a0') to text.substringAfter('\u00a0').uppercase()
        }
        StatsColumn(
            numberText = savedCount,
            labelText = savedText,
        )
    }
}

@Composable
private fun RowScope.StatsColumn(
    numberText: String,
    labelText: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
    ) {
        TextP30(
            text = numberText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        TextP60(
            text = labelText,
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

internal data class ProfileStatsState(
    val podcastsCount: Int,
    val listenedDuration: Duration,
    val savedDuration: Duration,
)

private val FriendlyDurationUnit.savedPlural
    get() = when (this) {
        FriendlyDurationUnit.Day -> LR.plurals.profile_stats_day_saved
        FriendlyDurationUnit.Hour -> LR.plurals.profile_stats_hour_saved
        FriendlyDurationUnit.Minute -> LR.plurals.profile_stats_minute_saved
        FriendlyDurationUnit.Second -> LR.plurals.profile_stats_second_saved
    }

private val FriendlyDurationUnit.listenedPlural
    get() = when (this) {
        FriendlyDurationUnit.Day -> LR.plurals.profile_stats_day_listened
        FriendlyDurationUnit.Hour -> LR.plurals.profile_stats_hour_listened
        FriendlyDurationUnit.Minute -> LR.plurals.profile_stats_minute_listened
        FriendlyDurationUnit.Second -> LR.plurals.profile_stats_second_listened
    }

@Preview
@Composable
private fun ProfileStatsPreview(
    @PreviewParameter(ProfileStatsStateParameterProvider::class) state: ProfileStatsState,
) {
    AppTheme(Theme.ThemeType.LIGHT) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(400.dp, 140.dp)
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(15.dp),
        ) {
            ProfileStats(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(fontScale = 1.5f)
@Composable
private fun ProfileStatsNarrowPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(400.dp)
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(15.dp),
        ) {
            ProfileStats(
                state = ProfileStatsState(
                    podcastsCount = Int.MAX_VALUE,
                    listenedDuration = Duration.INFINITE,
                    savedDuration = 10.hours,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileStatsThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppTheme(theme) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            ProfileStats(
                state = ProfileStatsState(
                    podcastsCount = 255,
                    listenedDuration = 125.days + 20.hours,
                    savedDuration = 20.hours + 3.minutes,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private class ProfileStatsStateParameterProvider : PreviewParameterProvider<ProfileStatsState> {
    override val values = sequenceOf(
        ProfileStatsState(
            podcastsCount = 0,
            listenedDuration = Duration.ZERO,
            savedDuration = Duration.ZERO,
        ),
        ProfileStatsState(
            podcastsCount = 1,
            listenedDuration = 1.days,
            savedDuration = 1.days,
        ),
        ProfileStatsState(
            podcastsCount = 1,
            listenedDuration = 1.hours,
            savedDuration = 1.hours,
        ),
        ProfileStatsState(
            podcastsCount = 1,
            listenedDuration = 1.minutes,
            savedDuration = 1.minutes,
        ),
        ProfileStatsState(
            podcastsCount = 1,
            listenedDuration = 1.seconds,
            savedDuration = 1.seconds,
        ),
        ProfileStatsState(
            podcastsCount = 2,
            listenedDuration = 2.days,
            savedDuration = 2.days,
        ),
        ProfileStatsState(
            podcastsCount = 2,
            listenedDuration = 2.hours,
            savedDuration = 2.hours,
        ),
        ProfileStatsState(
            podcastsCount = 2,
            listenedDuration = 2.minutes,
            savedDuration = 2.minutes,
        ),
        ProfileStatsState(
            podcastsCount = 2,
            listenedDuration = 2.seconds,
            savedDuration = 2.seconds,
        ),
    )
}
