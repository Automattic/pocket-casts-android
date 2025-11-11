package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

sealed interface Story {
    val previewDuration: Duration? get() = 10.seconds
    val isFree: Boolean get() = true
    val isShareble: Boolean get() = true
    val analyticsValue: String

    data object Cover : Story {
        override val isShareble = false
        override val analyticsValue = "cover"
    }

    data class NumberOfShows(
        val showCount: Int,
        val epsiodeCount: Int,
        val topShowIds: List<String>,
        val bottomShowIds: List<String>,
    ) : Story {
        override val analyticsValue = "number_of_shows"
    }

    data class TopShow(
        val show: TopPodcast,
    ) : Story {
        override val analyticsValue = "top_1_show"
    }

    data class TopShows(
        val shows: List<TopPodcast>,
        val podcastListUrl: String?,
    ) : Story {
        override val analyticsValue = "top_5_shows"
    }

    data class Ratings(
        val stats: RatingStats,
    ) : Story {
        override val isShareble get() = stats.max().second != 0
        override val analyticsValue = "ratings"
    }

    data class TotalTime(
        val duration: Duration,
    ) : Story {
        override val analyticsValue = "total_time"
    }

    data class LongestEpisode(
        val episode: LongestEpisodeData,
    ) : Story {
        override val analyticsValue = "longest_episode"
    }

    data object PlusInterstitial : Story {
        override val previewDuration = null
        override val isShareble = false
        override val analyticsValue = "plus_interstitial"
    }

    data class YearVsYear(
        val lastYearDuration: Duration,
        val thisYearDuration: Duration,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val isFree = false
        override val analyticsValue = "year_vs_year"

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
        override val analyticsValue = "completion_rate"

        val completionRate
            get() = when {
                listenedCount == 0 -> 1.0
                else -> completedCount.toDouble() / listenedCount
            }
    }

    data object Ending : Story {
        override val isShareble get() = false
        override val analyticsValue = "ending"
    }
}
