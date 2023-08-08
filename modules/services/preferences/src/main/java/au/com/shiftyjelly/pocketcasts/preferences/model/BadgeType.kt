package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

enum class BadgeType(
    internal val persistedInt: Int,
    @StringRes val labelId: Int,
    val analyticsValue: String,
) {
    OFF(
        persistedInt = 0,
        labelId = R.string.podcasts_badges_off,
        analyticsValue = "off",
    ),

    LATEST_EPISODE(
        persistedInt = 1,
        labelId = R.string.podcasts_badges_only_latest_episode,
        analyticsValue = "only_latest_episode",
    ),

    ALL_UNFINISHED(
        persistedInt = 2,
        labelId = R.string.podcasts_badges_all_unfinished,
        analyticsValue = "unfinished_episodes",
    );

    companion object {
        val defaultValue = OFF

        fun fromPersistedInt(value: Int): BadgeType =
            BadgeType.values().find { it.persistedInt == value }
                ?: run {
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Unknown persisted int for badge type: $value")
                    defaultValue
                }
    }
}
