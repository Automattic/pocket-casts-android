package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.Context
import android.media.AudioManager
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class NotificationVibrateSetting(
    val intValue: Int,
    @StringRes val summary: Int,
) {

    Never(
        intValue = 0,
        summary = LR.string.settings_notification_vibrate_never,
    ) {
        override fun isNotificationVibrateOn(context: Context) = false
    },

    OnlyWhenSilent(
        intValue = 1,
        summary = LR.string.settings_notification_vibrate_in_silent,
    ) {

        override fun isNotificationVibrateOn(context: Context): Boolean {
            val isSoundOn = AudioManager.RINGER_MODE_NORMAL == (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode
            return !isSoundOn
        }
    },

    NewEpisodes(
        intValue = 2,
        summary = LR.string.settings_notification_vibrate_new_episodes,
    ) {
        override fun isNotificationVibrateOn(context: Context) = true
    };

    abstract fun isNotificationVibrateOn(context: Context): Boolean

    companion object {
        val DEFAULT = NewEpisodes
    }
}
