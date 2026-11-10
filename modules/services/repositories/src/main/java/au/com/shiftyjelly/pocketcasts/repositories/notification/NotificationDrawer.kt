package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Notification
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.session.MediaSession

interface NotificationDrawer {

    fun buildPlayingNotification(
        sessionToken: MediaSessionCompat.Token,
        useEpisodeArtwork: Boolean,
    ): Notification

    fun buildPreparingNotification(
        sessionToken: MediaSessionCompat.Token,
    ): Notification

    fun buildPreparingNotification(
        session: MediaSession?,
        title: String?,
    ): Notification
}
