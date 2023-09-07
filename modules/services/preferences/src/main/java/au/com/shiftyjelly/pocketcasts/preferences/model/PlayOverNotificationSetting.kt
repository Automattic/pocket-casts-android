package au.com.shiftyjelly.pocketcasts.preferences.model

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class PlayOverNotificationSetting(
    val preferenceInt: Int,
    @StringRes val titleRes: Int,
    val analyticsString: String,
) {
    NEVER(
        titleRes = LR.string.settings_notification_play_over_never,
        preferenceInt = 2,
        analyticsString = "never"
    ),
    DUCK(
        titleRes = LR.string.settings_notification_play_over_duck,
        preferenceInt = 1,
        analyticsString = "duck"
    ),
    ALWAYS(
        titleRes = LR.string.settings_notification_play_over_always,
        preferenceInt = 0,
        analyticsString = "always"
    );

    companion object {
        fun fromPreferenceString(stringValue: String): PlayOverNotificationSetting {
            try {
                val intValue = stringValue.toInt()
                return values().first { it.preferenceInt == intValue }
            } catch (e: Exception) {
                throw IllegalStateException("Unknown play over notification setting: $stringValue")
            }
        }
    }
}
