package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.automattic.eventhorizon.EndOfYearStoryType
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

sealed interface Story {
    val previewDuration: Duration? get() = 7.seconds
    val isFree: Boolean get() = true
    val isShareable: Boolean get() = true
    val eventHorizonValue: EndOfYearStoryType
    val analyticsValue get() = eventHorizonValue.toString()

    data object PlaceholderWhileLoading : Story {
        override val isShareable = false
        override val eventHorizonValue get() = EndOfYearStoryType.LoadingPlaceholder
        override val previewDuration: Duration
            get() = Duration.INFINITE
    }

    data object Cover : Story {
        override val isShareable = false
        override val eventHorizonValue get() = EndOfYearStoryType.Cover
    }

    data class NumberOfShows(
        val showCount: Int,
        val episodeCount: Int,
        val randomShowIds: List<String>,
    ) : Story {
        override val eventHorizonValue get() = EndOfYearStoryType.NumberOfShows
    }

    data class TopShow(
        val show: TopPodcast,
    ) : Story {
        override val eventHorizonValue get() = EndOfYearStoryType.Top1Show
    }

    data class TopShows(
        val shows: List<TopPodcast>,
        val podcastListUrl: String?,
    ) : Story {
        override val eventHorizonValue get() = EndOfYearStoryType.Top5Shows
    }

    data class Ratings(
        val stats: RatingStats,
    ) : Story {
        override val isShareable get() = stats.max().second != 0
        override val eventHorizonValue get() = EndOfYearStoryType.Ratings
    }

    data class TotalTime(
        val duration: Duration,
    ) : Story {
        override val eventHorizonValue get() = EndOfYearStoryType.TotalTime
    }

    data class LongestEpisode(
        val episode: LongestEpisodeData,
    ) : Story {
        override val eventHorizonValue get() = EndOfYearStoryType.LongestEpisode
    }

    data class PlusInterstitial(
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val previewDuration = null
        override val isShareable = false
        override val eventHorizonValue get() = EndOfYearStoryType.PlusInterstitial
    }

    data class YearVsYear(
        val lastYearDuration: Duration,
        val thisYearDuration: Duration,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val isFree = false
        override val eventHorizonValue get() = EndOfYearStoryType.YearVsYear

        val percentageChange = when (lastYearDuration) {
            thisYearDuration -> 0
            Duration.ZERO -> Int.MAX_VALUE
            else -> (((thisYearDuration - lastYearDuration) / lastYearDuration) * 100.00).roundToInt()
        }

        val ratioChange = when (lastYearDuration) {
            thisYearDuration -> 1.0
            Duration.ZERO -> Double.POSITIVE_INFINITY
            else -> thisYearDuration / lastYearDuration
        }

        val trend = when {
            ratioChange < 0.9 -> Trend.Down
            ratioChange <= 1.1 -> Trend.Same
            ratioChange < 5.0 -> Trend.Up
            else -> Trend.UpALot
        }

        enum class Trend {
            Down,
            Same,
            Up,
            UpALot,
        }
    }

    data class CompletionRate(
        val listenedCount: Int,
        val completedCount: Int,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val isFree = false
        override val eventHorizonValue get() = EndOfYearStoryType.CompletionRate

        val completionRate
            get() = when {
                listenedCount == 0 -> 1.0
                else -> completedCount.toDouble() / listenedCount
            }

        val completionRatePercentage
            get() = (completionRate * 100).roundToInt()
    }

    data object Ending : Story {
        override val isShareable get() = false
        override val eventHorizonValue get() = EndOfYearStoryType.Ending
    }
}
