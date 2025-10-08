package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class UpNextSortType(
    val analyticsValue: String,
    @StringRes val descriptionId: Int,
) : Comparator<BaseEpisode> {
    NewestToOldest(
        analyticsValue = "newest_to_oldest",
        descriptionId = LR.string.sort_newest_to_oldest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o2, o1)
        }
    },
    OldestToNewest(
        analyticsValue = "oldest_to_newest",
        descriptionId = LR.string.sort_oldest_to_newest,
    ) {
        private val comparatorDelegate = Comparator.comparing(BaseEpisode::publishedDate)
            .thenComparing(BaseEpisode::addedDate)

        override fun compare(o1: BaseEpisode, o2: BaseEpisode): Int {
            return comparatorDelegate.compare(o1, o2)
        }
    },
}
