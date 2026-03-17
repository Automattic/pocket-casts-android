package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import com.automattic.eventhorizon.PlayOverNotificationType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class PlayOverNotificationSetting(
    val preferenceInt: Int,
    val serverId: Int,
    @StringRes val titleRes: Int,
    val analyticsValue: PlayOverNotificationType,
) {
    NEVER(
        preferenceInt = 2,
        serverId = 0,
        titleRes = LR.string.settings_notification_play_over_never,
        analyticsValue = PlayOverNotificationType.Never,
    ),
    DUCK(
        preferenceInt = 1,
        serverId = 2,
        titleRes = LR.string.settings_notification_play_over_duck,
        analyticsValue = PlayOverNotificationType.Duck,
    ),
    ALWAYS(
        preferenceInt = 0,
        serverId = 1,
        titleRes = LR.string.settings_notification_play_over_always,
        analyticsValue = PlayOverNotificationType.Always,
    ),
    ;

    companion object {
        fun fromPreferenceString(stringValue: String): PlayOverNotificationSetting {
            try {
                val intValue = stringValue.toInt()
                return entries.first { it.preferenceInt == intValue }
            } catch (e: Exception) {
                throw IllegalStateException("Unknown play over notification setting: $stringValue")
            }
        }

        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: NEVER
    }
}
