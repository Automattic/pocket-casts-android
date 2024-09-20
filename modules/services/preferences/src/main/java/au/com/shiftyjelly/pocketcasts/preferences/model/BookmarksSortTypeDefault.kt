package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting

// Be careful changing these values because we also use them as properties for Tracks
private const val SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST = "date_added_newest_to_oldest"
private const val SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST = "date_added_oldest_to_newest"
private const val SORT_TYPE_TIMESTAMP = "timestamp"

enum class BookmarksSortTypeDefault(
    override val labelId: Int,
    override val key: String,
    val serverId: Int,
) : BookmarksSortType {
    DATE_ADDED_NEWEST_TO_OLDEST(
        labelId = R.string.bookmarks_sort_newest_to_oldest,
        key = SORT_TYPE_DATE_ADDED_NEWEST_TO_OLDEST,
        serverId = 0,
    ),
    DATE_ADDED_OLDEST_TO_NEWEST(
        labelId = R.string.bookmarks_sort_oldest_to_newest,
        key = SORT_TYPE_DATE_ADDED_OLDEST_TO_NEWEST,
        serverId = 1,
    ),
    TIMESTAMP(
        labelId = R.string.bookmarks_sort_timestamp,
        key = SORT_TYPE_TIMESTAMP,
        serverId = 2,
    ),
    ;

    class UserSettingPref(
        sharedPrefKey: String,
        defaultValue: BookmarksSortTypeDefault,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<BookmarksSortTypeDefault>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromString = { str ->
            BookmarksSortTypeDefault.entries.find { it.key == str } ?: defaultValue
        },
        toString = { it.key },
    )

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
