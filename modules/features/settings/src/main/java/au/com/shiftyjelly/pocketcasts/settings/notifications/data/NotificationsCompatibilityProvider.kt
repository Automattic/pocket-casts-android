package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.os.Build
import javax.inject.Inject

internal class NotificationsCompatibilityProvider(val hasNotificationChannels: Boolean) {
    @Inject
    constructor() : this(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
}
