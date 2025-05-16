package au.com.shiftyjelly.pocketcasts.settings.notifications.model

internal data class NotificationPreferenceCategory(
    val title: String,
    val preferences: List<NotificationPreferenceType>,
)
