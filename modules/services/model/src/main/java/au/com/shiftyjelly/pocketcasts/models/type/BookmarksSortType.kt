package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST = "date_added_newest_to_oldest"
private const val SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST = "date_added_oldest_to_newest"
private const val SORT_TYPE_EPISODE = "episode"
private const val SORT_TYPE_TIMESTAMP = "timestamp"

interface BookmarksSortType {
    val labelId: Int
    val key: String
}

enum class BookmarksSortTypeForPlayer(
    override val labelId: Int,
    override val key: String,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = LR.string.bookmarks_sort_newest_to_oldest,
        key = SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = LR.string.bookmarks_sort_oldest_to_newest,
        key = SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST
    ),
    TIMESTAMP(
        labelId = LR.string.bookmarks_sort_timestamp,
        key = SORT_TYPE_TIMESTAMP
    );

    companion object {
        fun fromString(key: String?) =
            BookmarksSortTypeForPlayer.values().find { it.key == key }
    }
}

enum class BookmarksSortTypeForPodcast(
    override val labelId: Int,
    override val key: String,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = LR.string.bookmarks_sort_newest_to_oldest,
        key = SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = LR.string.bookmarks_sort_oldest_to_newest,
        key = SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST
    ),
    EPISODE(
        labelId = LR.string.episode,
        key = SORT_TYPE_EPISODE
    );

    companion object {
        fun fromString(key: String?) =
            BookmarksSortTypeForPodcast.values().find { it.key == key }
    }
}
