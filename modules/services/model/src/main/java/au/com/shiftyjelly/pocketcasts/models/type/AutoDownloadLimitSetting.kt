package au.com.shiftyjelly.pocketcasts.models.type

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoDownloadLimitSetting(
    val id: Int,
    val episodeCount: Int,
    @StringRes val titleRes: Int,
    @StringRes val episodeCountRes: Int,
) {
    LATEST_EPISODE(
        id = 1,
        episodeCount = 1,
        titleRes = LR.string.settings_auto_download_limit_latest_episode,
        episodeCountRes = LR.string.number_one,
    ),
    TWO_LATEST_EPISODE(
        id = 2,
        episodeCount = 2,
        titleRes = LR.string.settings_auto_download_limit_two_latest_episode,
        episodeCountRes = LR.string.number_two,
    ),
    THREE_LATEST_EPISODE(
        id = 3,
        episodeCount = 3,
        titleRes = LR.string.settings_auto_download_limit_three_latest_episode,
        episodeCountRes = LR.string.number_three,
    ),
    FIVE_LATEST_EPISODE(
        id = 4,
        episodeCount = 5,
        titleRes = LR.string.settings_auto_download_limit_five_latest_episode,
        episodeCountRes = LR.string.number_five,
    ),
    TEN_LATEST_EPISODE(
        id = 5,
        episodeCount = 10,
        titleRes = LR.string.settings_auto_download_limit_ten_latest_episode,
        episodeCountRes = LR.string.number_ten,
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
    }
}
