package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import com.automattic.eventhorizon.UpNextSortOrderType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class UpNextSortType(
    val analyticsValue: UpNextSortOrderType,
    @StringRes val descriptionId: Int,
) : Comparator<BaseEpisode> {
    NewestToOldest(
        analyticsValue = UpNextSortOrderType.NewestToOldest,
        descriptionId = LR.string.sort_newest_to_oldest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o2, o1)
        }
    },
    OldestToNewest(
        analyticsValue = UpNextSortOrderType.OldestToNewest,
        descriptionId = LR.string.sort_oldest_to_newest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o1, o2)
        }
    },
    ShortestToLongest(
        analyticsValue = UpNextSortOrderType.ShortestToLongest,
        descriptionId = LR.string.sort_shortest_to_longest,
    ) {
        // Episodes without a known duration always sort to the bottom.
        private val comparatorDelegate = compareBy<BaseEpisode> { it.hasNoDuration }
            .thenBy { it.timeRemaining }
            .thenBy { it.addedDate }

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o1, o2)
        }
    },
    LongestToShortest(
        analyticsValue = UpNextSortOrderType.LongestToShortest,
        descriptionId = LR.string.sort_longest_to_shortest,
    ) {
        // Episodes without a known duration always sort to the bottom.
        private val comparatorDelegate = compareBy<BaseEpisode> { it.hasNoDuration }
            .thenByDescending { it.timeRemaining }
            .thenBy { it.addedDate }

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o1, o2)
        }
    },
    ;

    private companion object {
        private val BaseEpisode.hasNoDuration get() = duration <= 0

        // Time remaining accounts for how much of the episode has already been played.
        private val BaseEpisode.timeRemaining get() = duration - playedUpTo
    }
}
