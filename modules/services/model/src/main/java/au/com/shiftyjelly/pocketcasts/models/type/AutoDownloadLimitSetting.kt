package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoDownloadLimitSetting(
    val id: Int,
    @StringRes val titleRes: Int,
) {
    LATEST_EPISODE(
        id = 1,
        titleRes = LR.string.settings_auto_download_limit_latest_episode,
    ),
    TWO_LATEST_EPISODE(
        id = 2,
        titleRes = LR.string.settings_auto_download_limit_two_latest_episode,
    ),
    THREE_LATEST_EPISODE(
        id = 3,
        titleRes = LR.string.settings_auto_download_limit_three_latest_episode,
    ),
    FIVE_LATEST_EPISODE(
        id = 4,
        titleRes = LR.string.settings_auto_download_limit_five_latest_episode,
    ),
    TEN_LATEST_EPISODE(
        id = 5,
        titleRes = LR.string.settings_auto_download_limit_ten_latest_episode,
    ),
    ;

    companion object {

        fun fromPreferenceString(stringValue: String): AutoDownloadLimitSetting? {
            return try {
                val intValue = stringValue.toInt()
                entries.firstOrNull { it.id == intValue }
            } catch (e: Exception) {
                null
            }
        }

        fun fromInt(id: Int) = (AutoDownloadLimitSetting.entries.firstOrNull { it.id == id })

        fun getNumberOfEpisodes(setting: AutoDownloadLimitSetting): Int = when (setting) {
            LATEST_EPISODE -> 1
            TWO_LATEST_EPISODE -> 2
            THREE_LATEST_EPISODE -> 3
            FIVE_LATEST_EPISODE -> 5
            TEN_LATEST_EPISODE -> 10
        }
    }
}
