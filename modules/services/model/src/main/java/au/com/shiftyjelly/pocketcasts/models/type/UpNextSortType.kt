package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import com.automattic.eventhorizon.UpNextSortOrderType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class UpNextSortType(
    val eventHorizonValue: UpNextSortOrderType,
    @StringRes val descriptionId: Int,
) : Comparator<BaseEpisode> {
    NewestToOldest(
        eventHorizonValue = UpNextSortOrderType.NewestToOldest,
        descriptionId = LR.string.sort_newest_to_oldest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o2, o1)
        }
    },
    OldestToNewest(
        eventHorizonValue = UpNextSortOrderType.OldestToNewest,
        descriptionId = LR.string.sort_oldest_to_newest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o1, o2)
        }
    },
}
