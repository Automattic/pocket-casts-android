package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Notification
import android.support.v4.media.session.MediaSessionCompat

interface NotificationDrawer {

    fun buildPlayingNotification(sessionToken: MediaSessionCompat.Token): Notification
}
