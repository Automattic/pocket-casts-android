package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.Util

sealed class AutoArchiveAfterPlayingSetting(val timeSeconds: Int, val analyticsValue: String) {
    companion object {
        fun defaultValue(context: Context) = if (Util.isAutomotive(context)) Never else AfterPlaying

        fun fromString(value: String?, context: Context): AutoArchiveAfterPlayingSetting =
            when (value) {
                context.getString(R.string.settings_auto_archive_played_never) -> Never
                context.getString(R.string.settings_auto_archive_played_after_playing) -> AfterPlaying
                context.getString(R.string.settings_auto_archive_played_after_24_hours) -> Hours24
                context.getString(R.string.settings_auto_archive_played_after_2_days) -> Days2
                context.getString(R.string.settings_auto_archive_played_after_1_week) -> Weeks1
                else -> defaultValue(context)
            }

        val options
            get() = listOf(Never, AfterPlaying, Hours24, Days2, Weeks1)

        fun fromIndex(index: Int) = options[index]
    }

    object Never : AutoArchiveAfterPlayingSetting(-1, "never")
    object AfterPlaying : AutoArchiveAfterPlayingSetting(0, "after_playing")
    object Hours24 : AutoArchiveAfterPlayingSetting(24 * 60 * 60, "after_24_hours")
    object Days2 : AutoArchiveAfterPlayingSetting(2 * 24 * 60 * 60, "after_2_days")
    object Weeks1 : AutoArchiveAfterPlayingSetting(7 * 24 * 60 * 60, "after_1_week")

    fun toIndex(): Int = options.indexOf(this)
}
