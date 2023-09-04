package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R

sealed class AutoArchiveInactiveSetting(val timeSeconds: Int, val analyticsValue: String) {
    object Never : AutoArchiveInactiveSetting(-1, "never")
    object Hours24 : AutoArchiveInactiveSetting(24 * 60 * 60, "after_24_hours")
    object Days2 : AutoArchiveInactiveSetting(2 * 24 * 60 * 60, "after_2_days")
    object Weeks1 : AutoArchiveInactiveSetting(7 * 24 * 60 * 60, "after_1_week")
    object Weeks2 : AutoArchiveInactiveSetting(14 * 24 * 60 * 60, "after_2_weeks")
    object Days30 : AutoArchiveInactiveSetting(30 * 24 * 60 * 60, "after_30_days")
    object Days90 : AutoArchiveInactiveSetting(90 * 24 * 60 * 60, "after_3_months")

    companion object {

        val default = Never

        fun fromString(value: String?, context: Context): AutoArchiveInactiveSetting {
            return when (value) {
                context.getString(R.string.settings_auto_archive_inactive_never) -> Never
                context.getString(R.string.settings_auto_archive_inactive_24_hours) -> Hours24
                context.getString(R.string.settings_auto_archive_inactive_2_days) -> Days2
                context.getString(R.string.settings_auto_archive_inactive_1_week) -> Weeks1
                context.getString(R.string.settings_auto_archive_inactive_2_weeks) -> Weeks2
                context.getString(R.string.settings_auto_archive_inactive_30_days) -> Days30
                context.getString(R.string.settings_auto_archive_inactive_3_months) -> Days90
                else -> default
            }
        }

        val options
            get() = listOf(Never, Hours24, Days2, Weeks1, Weeks2, Days30, Days90)

        fun fromIndex(index: Int) = options[index]
    }

    fun toIndex(): Int = options.indexOf(this)
}
