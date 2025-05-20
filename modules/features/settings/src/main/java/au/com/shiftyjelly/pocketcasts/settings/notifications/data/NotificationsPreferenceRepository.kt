package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType

internal interface NotificationsPreferenceRepository {

    suspend fun getPreferenceCategories(): List<NotificationPreferenceCategory>
    suspend fun setPreference(preference: NotificationPreferenceType)
}
