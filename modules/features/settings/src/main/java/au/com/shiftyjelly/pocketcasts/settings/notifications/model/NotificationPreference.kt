package au.com.shiftyjelly.pocketcasts.settings.notifications.model

internal sealed interface NotificationPreference {
    val title: String

    sealed interface ValuePreference<T> : NotificationPreference, LocalPreference {
        val value: T

        sealed interface TextPreference : ValuePreference<String> {
            data class MultiSelectPreference(
                override val title: String,
                override val value: String,
                override val preferenceKey: String,
            ) : TextPreference

            data class SingleSelectPreference(
                override val title: String,
                override val value: String,
                override val preferenceKey: String,
            ) : TextPreference
        }

        data class SwitchPreference(
            override val title: String,
            override val value: Boolean,
            override val preferenceKey: String,
        ) : ValuePreference<Boolean>
    }

    data class ExternalPreference(
        override val title: String,
        val description: String,
    ) : NotificationPreference
}

internal interface LocalPreference {
    val preferenceKey: String
}