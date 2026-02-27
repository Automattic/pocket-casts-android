package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import com.automattic.eventhorizon.BookmarkSortType

private const val SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST = "date_added_newest_to_oldest"
private const val SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST = "date_added_oldest_to_newest"
private const val SORT_TYPE_PODCAST_AND_EPISODE = "podcast_and_episode"

enum class BookmarksSortTypeForProfile(
    override val labelId: Int,
    override val key: String,
    override val eventHorizonValue: BookmarkSortType,
    val serverId: Int,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = R.string.bookmarks_sort_newest_to_oldest,
        key = SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST,
        eventHorizonValue = BookmarkSortType.DateAddedNewestToOldest,
        serverId = 0,
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = R.string.bookmarks_sort_oldest_to_newest,
        key = SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST,
        eventHorizonValue = BookmarkSortType.DateAddedOldestToNewest,
        serverId = 1,
    ),
    PODCAST_AND_EPISODE(
        labelId = R.string.podcast_and_episode,
        key = SORT_TYPE_PODCAST_AND_EPISODE,
        eventHorizonValue = BookmarkSortType.PodcastAndEpisode,
        serverId = 2,
    ),
    ;

    class UserSettingPref(
        sharedPrefKey: String,
        defaultValue: BookmarksSortTypeForProfile,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<BookmarksSortTypeForProfile>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromString = { str ->
            BookmarksSortTypeForProfile.entries.find { it.key == str } ?: defaultValue
        },
        toString = { it.key },
    )

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
