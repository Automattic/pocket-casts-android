package au.com.shiftyjelly.pocketcasts.settings.notifications.model

/**
 * This enum defines the preferences we keep record on
 */
internal enum class NotificationPreferences {
    NEW_EPISODES_NOTIFY_ME,
    NEW_EPISODES_CHOOSE_PODCASTS,
    NEW_EPISODES_ACTIONS,
    NEW_EPISODES_ADVANCED,
    NEW_EPISODES_RINGTONE,
    NEW_EPISODES_VIBRATION,
    SETTINGS_PLAY_OVER,
    SETTINGS_HIDE_NOTIFICATION_ON_PAUSE
}

/**
 * Generic interface that describes the common preference properties and constrain subclassing via its sealed nature.
 */
internal sealed interface NotificationPreference<T> {
    val title: String
    val preference: NotificationPreferences
    val value: T

    /**
     * Simple preference that stores a boolean value that can either be on/off.
     */
    data class SwitchPreference(
        override val title: String,
        override val value: Boolean,
        override val preference: NotificationPreferences
    ) : NotificationPreference<Boolean>

    /**
     * Simple preference that represents a preference with a human-readable value.
     */
    data class TextPreference(
        override val title: String,
        override val value: String?,
        override val preference: NotificationPreferences,
    ): NotificationPreference<String?>

    /**
     * Preference that holds the generic raw value of the underlying setting and also provides a human-readable [displayValue].
     */
    data class ValueHolderPreference<T>(
        override val title: String,
        override val value: T,
        val displayValue: String,
        override val preference: NotificationPreferences,
    ): NotificationPreference<T>

    /**
     * Preference that lets you select form the provided [options] and keeps track of the current selection. Also provides a human-readable [displayText] that represents the current [value].
     */
    data class RadioGroupPreference<T>(
        override val title: String,
        override val value: T,
        override val preference: NotificationPreferences,
        val options: List<T>,
        val displayText: String?
    ) : NotificationPreference<T>

    /**
     * Preference that lets you select at most [maxNumberOfSelectableOptions] options from the provided [options] and keeps track of the current selection. Also human-readable [displayText] that represents the current [value].
     */
    data class MultiSelectPreference<T>(
        override val title: String,
        override val value: List<T>,
        override val preference: NotificationPreferences,
        val options: List<T>,
        val displayText: String?,
        val maxNumberOfSelectableOptions: Int
    ) : NotificationPreference<List<T>>
}
