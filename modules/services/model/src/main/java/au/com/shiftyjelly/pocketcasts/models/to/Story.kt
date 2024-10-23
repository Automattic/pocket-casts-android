package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

sealed interface Story {
    val previewDuration: Duration? get() = 7.seconds
    val isFree: Boolean get() = true

    data object Cover : Story

    data class NumberOfShows(
        val showCount: Int,
        val epsiodeCount: Int,
        val topShowIds: List<String>,
        val bottomShowIds: List<String>,
    ) : Story

    data class TopShow(
        val show: TopPodcast,
    ) : Story

    data class TopShows(
        val shows: List<TopPodcast>,
    ) : Story

    data class Ratings(
        val stats: RatingStats,
    ) : Story

    data class TotalTime(
        val duration: Duration,
    ) : Story

    data class LongestEpisode(
        val episode: LongestEpisodeData,
    ) : Story

    data object PlusInterstitial : Story {
        override val previewDuration = null
    }

    data class YearVsYear(
        val lastYearDuration: Duration,
        val thisYearDuration: Duration,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val isFree = false

        val yearOverYearChange
            get() = when {
                lastYearDuration == thisYearDuration -> 1.0
                lastYearDuration == Duration.ZERO -> Double.POSITIVE_INFINITY
                else -> thisYearDuration / lastYearDuration
            }
    }

    data class CompletionRate(
        val listenedCount: Int,
        val completedCount: Int,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        override val isFree = false

        val completionRate
            get() = when {
                listenedCount == 0 -> 1.0
                else -> completedCount.toDouble() / listenedCount
            }
    }

    data object Ending : Story
}
