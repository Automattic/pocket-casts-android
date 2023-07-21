package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class BookmarksSortType(
    val labelId: Int,
    val analyticsValue: String,
) {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = LR.string.bookmarks_sort_newest_to_oldest,
        analyticsValue = "date_added_newest_to_oldest"
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = LR.string.bookmarks_sort_oldest_to_newest,
        analyticsValue = "date_added_oldest_to_newest"
    ),
    TIMESTAMP(
        labelId = LR.string.bookmarks_sort_timestamp,
        analyticsValue = "timestamp"
    );

    fun mapToLocalizedString() = when (this) {
        DATE_ADDED_OLDEST_TO_NEWEST -> LR.string.bookmarks_sort_oldest_to_newest
        DATE_ADDED_NEWEST_TO_OLDEST -> LR.string.bookmarks_sort_newest_to_oldest
        TIMESTAMP -> LR.string.bookmarks_sort_timestamp
    }
}
