package au.com.shiftyjelly.pocketcasts.settings.notifications.model

import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource

internal sealed interface NotificationPreferenceType {
    val title: TextResource

    data class NotifyMeOnNewEpisodes(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class NotifyOnThesePodcasts(
        override val title: TextResource,
        val displayValue: TextResource,
    ) : NotificationPreferenceType

    data class NotificationActions(
        override val title: TextResource,
        val value: List<NewEpisodeNotificationAction>,
        val options: List<NewEpisodeNotificationAction>,
        val displayValue: String,
        val selectableItemCount: Int = 3,
    ) : NotificationPreferenceType

    data class NotificationSoundPreference(
        override val title: TextResource,
        val notificationSound: NotificationSound,
        val displayedSoundName: String,
    ) : NotificationPreferenceType

    data class NotificationVibration(
        override val title: TextResource,
        val value: NotificationVibrateSetting,
        val displayValue: TextResource,
        val options: List<NotificationVibrateSetting> = NotificationVibrateSetting.entries,
    ) : NotificationPreferenceType

    data class AdvancedSettings(
        override val title: TextResource,
        val description: TextResource,
    ) : NotificationPreferenceType

    data class PlayOverNotifications(
        override val title: TextResource,
        val value: PlayOverNotificationSetting,
        val displayValue: TextResource,
        val options: List<PlayOverNotificationSetting> = PlayOverNotificationSetting.entries,
    ) : NotificationPreferenceType

    data class HidePlaybackNotificationOnPause(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class EnableDailyReminders(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class DailyReminderSettings(
        override val title: TextResource,
        val description: TextResource,
    ) : NotificationPreferenceType

    data class EnableRecommendations(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class RecommendationSettings(
        override val title: TextResource,
        val description: TextResource,
    ) : NotificationPreferenceType

    data class EnableNewFeaturesAndTips(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class NewFeaturesAndTipsSettings(
        override val title: TextResource,
        val description: TextResource,
    ) : NotificationPreferenceType

    data class EnableOffers(
        override val title: TextResource,
        val isEnabled: Boolean,
    ) : NotificationPreferenceType

    data class OffersSettings(
        override val title: TextResource,
        val description: TextResource,
    ) : NotificationPreferenceType
}
