package au.com.shiftyjelly.pocketcasts.settings.notifications.model

import au.com.shiftyjelly.pocketcasts.settings.util.TextResource

internal data class NotificationPreferenceCategory(
    val title: TextResource,
    val preferences: List<NotificationPreferenceType>,
)
