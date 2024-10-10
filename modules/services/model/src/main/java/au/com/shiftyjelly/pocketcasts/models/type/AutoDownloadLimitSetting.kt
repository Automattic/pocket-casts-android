package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoDownloadLimitSetting(
    val id: Int,
    @StringRes val titleRes: Int,
    val analyticsString: String,
) {
    OFF(
        id = 0,
        titleRes = LR.string.settings_auto_download_limit_off,
        analyticsString = "off",
    ),
    LATEST_EPISODE(
        id = 1,
        titleRes = LR.string.settings_auto_download_limit_latest_episode,
        analyticsString = "latest_episode",
    ),
    TWO_LATEST_EPISODE(
        id = 2,
        titleRes = LR.string.settings_auto_download_limit_two_latest_episode,
        analyticsString = "two_latest_episode",
    ),
    THREE_LATEST_EPISODE(
        id = 3,
        titleRes = LR.string.settings_auto_download_limit_three_latest_episode,
        analyticsString = "three_latest_episode",
    ),
    FIVE_LATEST_EPISODE(
        id = 4,
        titleRes = LR.string.settings_auto_download_limit_five_latest_episode,
        analyticsString = "five_latest_episode",
    ),
    TEN_LATEST_EPISODE(
        id = 5,
        titleRes = LR.string.settings_auto_download_limit_ten_latest_episode,
        analyticsString = "ten_latest_episode",
    ),
    ALL_LATEST_EPISODES(
        id = 6,
        titleRes = LR.string.settings_auto_download_limit_all_latest_episodes,
        analyticsString = "all_latest_episodes",
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
            OFF -> 0
            LATEST_EPISODE -> 1
            TWO_LATEST_EPISODE -> 2
            THREE_LATEST_EPISODE -> 3
            FIVE_LATEST_EPISODE -> 5
            TEN_LATEST_EPISODE -> 10
            ALL_LATEST_EPISODES -> 1 // todo - confirm what does all latest episodes mean
        }
    }
}
