package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val SORT_ORDER_DATE_ADDED_NEWEST_TO_OLDEST = "date_added_newest_to_oldest"
private const val SORT_ORDER_DATE_ADDED_OLDEST_TO_NEWEST = "date_added_oldest_to_newest"
private const val SORT_ORDER_EPISODE = "episode"
private const val SORT_ORDER_TIMESTAMP = "timestamp"

interface BookmarksSortType {
    val labelId: Int
    val analyticsValue: String
}

enum class BookmarksSortTypeForPlayer(
    override val labelId: Int,
    override val analyticsValue: String,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = LR.string.bookmarks_sort_newest_to_oldest,
        analyticsValue = SORT_ORDER_DATE_ADDED_NEWEST_TO_OLDEST
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = LR.string.bookmarks_sort_oldest_to_newest,
        analyticsValue = SORT_ORDER_DATE_ADDED_OLDEST_TO_NEWEST
    ),
    TIMESTAMP(
        labelId = LR.string.bookmarks_sort_timestamp,
        analyticsValue = SORT_ORDER_TIMESTAMP
    );
}

enum class BookmarksSortTypeForPodcast(
    override val labelId: Int,
    override val analyticsValue: String,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = LR.string.bookmarks_sort_newest_to_oldest,
        analyticsValue = SORT_ORDER_DATE_ADDED_NEWEST_TO_OLDEST
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = LR.string.bookmarks_sort_oldest_to_newest,
        analyticsValue = SORT_ORDER_DATE_ADDED_OLDEST_TO_NEWEST
    ),
    EPISODE(
        labelId = LR.string.episode,
        analyticsValue = SORT_ORDER_EPISODE
    );
}
