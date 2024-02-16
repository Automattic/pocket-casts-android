package au.com.shiftyjelly.pocketcasts.models.to

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

sealed class AutoArchiveInactive(
    val timeSeconds: Int,
    val serverId: Int,
    val index: Int,
    val analyticsValue: String,
) {
    companion object {
        val All get() = listOf(Never, Hours24, Days2, Weeks1, Weeks2, Days30, Days90)

        val Default get() = Never

        fun fromString(value: String?, context: Context): AutoArchiveInactive {
            return when (value) {
                context.getString(R.string.settings_auto_archive_inactive_never) -> Never
                context.getString(R.string.settings_auto_archive_inactive_24_hours) -> Hours24
                context.getString(R.string.settings_auto_archive_inactive_2_days) -> Days2
                context.getString(R.string.settings_auto_archive_inactive_1_week) -> Weeks1
                context.getString(R.string.settings_auto_archive_inactive_2_weeks) -> Weeks2
                context.getString(R.string.settings_auto_archive_inactive_30_days) -> Days30
                context.getString(R.string.settings_auto_archive_inactive_3_months) -> Days90
                else -> Default
            }
        }

        fun fromServerId(id: Int) = All.find { it.serverId == id }

        fun fromIndex(index: Int) = All.find { it.index == index }
    }

    data object Never : AutoArchiveInactive(
        timeSeconds = -1,
        serverId = 0,
        index = 0,
        analyticsValue = "never",
    )

    data object Hours24 : AutoArchiveInactive(
        timeSeconds = 24.hours.inWholeSeconds.toInt(),
        serverId = 1,
        index = 1,
        analyticsValue = "after_24_hours",
    )

    data object Days2 : AutoArchiveInactive(
        timeSeconds = 2.days.inWholeSeconds.toInt(),
        serverId = 2,
        index = 2,
        analyticsValue = "after_2_days",
    )

    data object Weeks1 : AutoArchiveInactive(
        timeSeconds = 7.days.inWholeSeconds.toInt(),
        serverId = 3,
        index = 3,
        analyticsValue = "after_1_week",
    )

    data object Weeks2 : AutoArchiveInactive(
        timeSeconds = 14.days.inWholeSeconds.toInt(),
        serverId = 4,
        index = 4,
        analyticsValue = "after_2_weeks",
    )

    data object Days30 : AutoArchiveInactive(
        timeSeconds = 30.days.inWholeSeconds.toInt(),
        serverId = 5,
        index = 5,
        analyticsValue = "after_30_days",
    )

    data object Days90 : AutoArchiveInactive(
        timeSeconds = 90.days.inWholeSeconds.toInt(),
        serverId = 6,
        index = 6,
        analyticsValue = "after_3_months",
    )
}
