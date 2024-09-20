package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.crashlogging.ObserveUser
import au.com.shiftyjelly.pocketcasts.crashlogging.User
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveTrackableUser @Inject constructor(
    private val settings: Settings,
    private val syncManager: SyncManager,
) : ObserveUser {
    override fun invoke(): Flow<User?> {
        return settings.linkCrashReportsToUser.flow.map { linkCrashReportsToUser ->
            if (linkCrashReportsToUser) {
                syncManager.getEmail()?.let {
                    User(it)
                }
            } else {
                null
            }
        }
    }
}
