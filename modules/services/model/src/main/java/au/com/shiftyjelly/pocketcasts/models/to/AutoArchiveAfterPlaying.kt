package au.com.shiftyjelly.pocketcasts.models.to

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

sealed class AutoArchiveAfterPlaying(
    val timeSeconds: Int,
    val serverId: Int,
    val index: Int,
    val analyticsValue: String,
) {
    companion object {
        fun defaultValue(context: Context) = if (Util.isAutomotive(context)) Never else AfterPlaying

        fun fromString(value: String?, context: Context): AutoArchiveAfterPlaying =
            when (value) {
                context.getString(R.string.settings_auto_archive_played_never) -> Never
                context.getString(R.string.settings_auto_archive_played_after_playing) -> AfterPlaying
                context.getString(R.string.settings_auto_archive_played_after_24_hours) -> Hours24
                context.getString(R.string.settings_auto_archive_played_after_2_days) -> Days2
                context.getString(R.string.settings_auto_archive_played_after_1_week) -> Weeks1
                else -> defaultValue(context)
            }

        val All
            get() = listOf(Never, AfterPlaying, Hours24, Days2, Weeks1)

        fun fromServerId(id: Int) = All.find { it.serverId == id }

        fun fromIndex(index: Int) = All.find { it.index == index }
    }

    data object Never : AutoArchiveAfterPlaying(
        timeSeconds = -1,
        serverId = 0,
        index = 0,
        analyticsValue = "never",
    )
    data object AfterPlaying : AutoArchiveAfterPlaying(
        timeSeconds = 0,
        serverId = 1,
        index = 1,
        analyticsValue = "after_playing",
    )
    data object Hours24 : AutoArchiveAfterPlaying(
        timeSeconds = 24.hours.inWholeSeconds.toInt(),
        serverId = 2,
        index = 2,
        analyticsValue = "after_24_hours",
    )
    data object Days2 : AutoArchiveAfterPlaying(
        timeSeconds = 2.days.inWholeSeconds.toInt(),
        serverId = 3,
        index = 3,
        analyticsValue = "after_2_days",
    )
    data object Weeks1 : AutoArchiveAfterPlaying(
        timeSeconds = 7.days.inWholeSeconds.toInt(),
        serverId = 4,
        index = 4,
        analyticsValue = "after_1_week",
    )
}
