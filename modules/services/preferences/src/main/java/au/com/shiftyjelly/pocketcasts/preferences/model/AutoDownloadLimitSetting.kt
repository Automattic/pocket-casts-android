package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoDownloadLimitSetting(
    val preferenceInt: Int,
    @StringRes val titleRes: Int,
) {
    OFF(
        preferenceInt = 0,
        titleRes = LR.string.settings_auto_download_limit_off,
    ),
    LATEST_EPISODE(
        preferenceInt = 1,
        titleRes = LR.string.settings_auto_download_limit_latest_episode,
    ),
    TWO_LATEST_EPISODE(
        preferenceInt = 2,
        titleRes = LR.string.settings_auto_download_limit_two_latest_episode,
    ),
    THREE_LATEST_EPISODE(
        preferenceInt = 3,
        titleRes = LR.string.settings_auto_download_limit_three_latest_episode,
    ),
    FIVE_LATEST_EPISODE(
        preferenceInt = 4,
        titleRes = LR.string.settings_auto_download_limit_five_latest_episode,
    ),
    TEN_LATEST_EPISODE(
        preferenceInt = 5,
        titleRes = LR.string.settings_auto_download_limit_ten_latest_episode,
    ),
    ALL_LATEST_EPISODES(
        preferenceInt = 6,
        titleRes = LR.string.settings_auto_download_limit_all_latest_episodes,
    ),
    ;

    companion object {

        fun fromPreferenceString(stringValue: String): AutoDownloadLimitSetting {
            try {
                val intValue = stringValue.toInt()
                return entries.first { it.preferenceInt == intValue }
            } catch (e: Exception) {
                throw IllegalStateException("Unknown auto download setting: $stringValue")
            }
        }
    }
}
