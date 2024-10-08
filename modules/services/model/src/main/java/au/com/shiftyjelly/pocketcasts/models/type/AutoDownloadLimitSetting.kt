package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoDownloadLimitSetting(
    val preferenceInt: Int,
    @StringRes val titleRes: Int,
    val analyticsString: String,
) {
    OFF(
        preferenceInt = 0,
        titleRes = LR.string.settings_auto_download_limit_off,
        analyticsString = "off",
    ),
    LATEST_EPISODE(
        preferenceInt = 1,
        titleRes = LR.string.settings_auto_download_limit_latest_episode,
        analyticsString = "latest_episode",
    ),
    TWO_LATEST_EPISODE(
        preferenceInt = 2,
        titleRes = LR.string.settings_auto_download_limit_two_latest_episode,
        analyticsString = "two_latest_episode",
    ),
    THREE_LATEST_EPISODE(
        preferenceInt = 3,
        titleRes = LR.string.settings_auto_download_limit_three_latest_episode,
        analyticsString = "three_latest_episode",
    ),
    FIVE_LATEST_EPISODE(
        preferenceInt = 4,
        titleRes = LR.string.settings_auto_download_limit_five_latest_episode,
        analyticsString = "five_latest_episode",
    ),
    TEN_LATEST_EPISODE(
        preferenceInt = 5,
        titleRes = LR.string.settings_auto_download_limit_ten_latest_episode,
        analyticsString = "ten_latest_episode",
    ),
    ALL_LATEST_EPISODES(
        preferenceInt = 6,
        titleRes = LR.string.settings_auto_download_limit_all_latest_episodes,
        analyticsString = "all_latest_episodes",
    ),
    ;

    companion object {

        fun fromPreferenceString(stringValue: String): AutoDownloadLimitSetting {
            return try {
                val intValue = stringValue.toInt()
                entries.firstOrNull { it.preferenceInt == intValue } ?: TWO_LATEST_EPISODE
            } catch (e: Exception) {
                TWO_LATEST_EPISODE
            }
        }
    }
}
