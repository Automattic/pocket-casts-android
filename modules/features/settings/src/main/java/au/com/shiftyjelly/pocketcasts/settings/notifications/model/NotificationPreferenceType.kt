package au.com.shiftyjelly.pocketcasts.settings.notifications.model

import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting

internal sealed interface NotificationPreferenceType {
    val title: String

    data class NotifyMeOnNewEpisodes(
        override val title: String,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class NotifyOnThesePodcasts(
        override val title: String,
        val displayValue: String,
    ) : NotificationPreferenceType

    data class NotificationActions(
        override val title: String,
        val value: List<NewEpisodeNotificationAction>,
        val options: List<NewEpisodeNotificationAction>,
        val displayValue: String,
        val selectableItemCount: Int = 3,
    ) : NotificationPreferenceType

    data class NotificationSoundPreference(
        override val title: String,
        val notificationSound: NotificationSound,
        val displayedSoundName: String,
    ) : NotificationPreferenceType

    data class NotificationVibration(
        override val title: String,
        val value: NotificationVibrateSetting,
        val displayValue: String,
        val options: List<NotificationVibrateSetting> = NotificationVibrateSetting.entries,
    ) : NotificationPreferenceType

    data class AdvancedSettings(
        override val title: String,
        val description: String,
    ) : NotificationPreferenceType

    data class PlayOverNotifications(
        override val title: String,
        val value: PlayOverNotificationSetting,
        val displayValue: String,
        val options: List<PlayOverNotificationSetting> = PlayOverNotificationSetting.entries,
    ) : NotificationPreferenceType

    data class HidePlaybackNotificationOnPause(
        override val title: String,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType
}
