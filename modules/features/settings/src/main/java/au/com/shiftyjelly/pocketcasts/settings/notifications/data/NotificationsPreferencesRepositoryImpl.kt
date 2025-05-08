package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.content.Context
import androidx.preference.PreferenceManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.LocalPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NotificationsPreferencesRepositoryImpl @Inject constructor(
    private val dispatcher: Dispatchers,
    private val context: Context,
) : NotificationsPreferenceRepository {

    private val preferenceManager by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override suspend fun getPreferenceCategories(): List<NotificationPreferenceCategory> = withContext(dispatcher.IO) {
        emptyList()
    }

    override suspend fun setPreference(preference: LocalPreference) = withContext(dispatcher.IO) {
        // TO be implemented later
    }
}