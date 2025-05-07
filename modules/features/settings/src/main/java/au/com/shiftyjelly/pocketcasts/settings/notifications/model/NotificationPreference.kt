package au.com.shiftyjelly.pocketcasts.settings.notifications.model

internal sealed interface NotificationPreference {
    val title: String

    sealed interface LocalPreference : NotificationPreference {
        val preferenceKey: String

        data class SwitchPreference(
            override val title: String,
            override val preferenceKey: String,
            val value: Boolean,
        ) : LocalPreference

        data class MultiSelectPreference(
            override val title: String,
            override val preferenceKey: String,
            val value: String,
        ) : LocalPreference

        data class SingleSelectPreference(
            override val title: String,
            override val preferenceKey: String,
            val value: String
        ) : LocalPreference
    }

    data class ExternalPreference(
        override val title: String,
        val description: String,
    ) : NotificationPreference
}