package au.com.shiftyjelly.pocketcasts.settings.notifications.model

internal enum class NotificationPreferences {
    NEW_EPISODES_NOTIFY_ME,
    NEW_EPISODES_CHOOSE_PODCASTS,
    NEW_EPISODES_ACTIONS,
    NEW_EPISODES_ADVANCED,
    NEW_EPISODES_RINGTONE,
    NEW_EPISODES_VIBRATION,
    SETTINGS_PlAY_OVER,
    SETTINGS_HIDE_NOTIFICATION_ON_PAUSE
}

internal sealed interface NotificationPreference<T> {
    val title: String
    val preference: NotificationPreferences
    val value: T

    data class SwitchPreference(
        override val title: String,
        override val value: Boolean,
        override val preference: NotificationPreferences
    ) : NotificationPreference<Boolean>

    data class TextPreference(
        override val title: String,
        override val value: String?,
        override val preference: NotificationPreferences,
    ): NotificationPreference<String?>

    data class RadioGroupPreference<T>(
        override val title: String,
        override val value: T,
        override val preference: NotificationPreferences,
        val options: List<T>,
        val displayText: String?
    ) : NotificationPreference<T>
}
