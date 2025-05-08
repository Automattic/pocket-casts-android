package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import au.com.shiftyjelly.pocketcasts.settings.notifications.model.LocalPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory

internal interface NotificationsPreferenceRepository {

    suspend fun getPreferenceCategories(): List<NotificationPreferenceCategory>
    suspend fun setPreference(preference: LocalPreference)

}