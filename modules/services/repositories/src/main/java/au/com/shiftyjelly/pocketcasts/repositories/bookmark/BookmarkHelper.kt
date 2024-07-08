package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import au.com.shiftyjelly.pocketcasts.deeplink.AddBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ChangeBookmarkTitleDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.DeleteBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.isAppForeground
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarkHelper @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val bookmarkManager: BookmarkManager,
    private val settings: Settings,
    private val bookmarkFeature: BookmarkFeatureControl,
) {
    suspend fun handleAddBookmarkAction(
        context: Context,
        isAndroidAutoConnected: Boolean,
    ) {
        if (!shouldAllowAddBookmark()) return
        if (context.isAppForeground() &&
            Util.getAppPlatform(context) == AppPlatform.Phone &&
            !isAndroidAutoConnected
        ) {
            val bookmarkIntent = AddBookmarkDeepLink.toIntent(context)
            context.startActivity(bookmarkIntent)
        } else {
            if (playbackManager.getCurrentEpisode() == null) return

            val episode = playbackManager.getCurrentEpisode() ?: return
            val timeInSecs = playbackManager.getCurrentTimeMs(episode) / 1000

            // Load existing bookmark
            var bookmark = bookmarkManager.findByEpisodeTime(
                episode = episode,
                timeSecs = timeInSecs,
            )

            if (bookmark == null) {
                bookmark = bookmarkManager.add(
                    episode = episode,
                    timeSecs = timeInSecs,
                    title = context.getString(LR.string.bookmark),
                    creationSource = BookmarkManager.CreationSource.HEADPHONES,
                )
            }

            if (settings.headphoneControlsPlayBookmarkConfirmationSound.value) {
                playbackManager.playBookmarkTone()
            }

            buildAndShowNotification(context, bookmark.uuid)
        }
    }

    private fun shouldAllowAddBookmark() =
        bookmarkFeature.isAvailable(settings.userTier)
}

private fun buildAndShowNotification(
    context: Context,
    bookmarkUuid: String,
) {
    val changeTitleAction = NotificationCompat.Action(
        IR.drawable.ic_notification_edit,
        context.getString(LR.string.bookmark_notification_action_change_title),
        buildPendingIntent(context, ChangeBookmarkTitleDeepLink(bookmarkUuid).toIntent(context)),
    )

    val deleteAction = NotificationCompat.Action(
        R.drawable.ic_delete_black,
        context.getString(LR.string.bookmark_notification_action_delete_title),
        buildPendingIntent(context, DeleteBookmarkDeepLink(bookmarkUuid).toIntent(context)),
    )

    val notification = NotificationCompat.Builder(
        context,
        Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id,
    )
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentTitle(context.getString(LR.string.bookmark_notification_title_added))
        .setContentText(context.getString(LR.string.bookmark_notification_content_tap_to_view))
        .setSmallIcon(IR.drawable.notification)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(buildPendingIntent(context, ShowBookmarkDeepLink(bookmarkUuid).toIntent(context)))
        .addAction(changeTitleAction)
        .addAction(deleteAction)
        .build()
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        NotificationManagerCompat.from(context)
            .notify(Settings.NotificationId.BOOKMARK.value, notification)
    } else {
        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Post notification permission not granted.")
    }
}

private fun buildPendingIntent(
    context: Context,
    intent: Intent?,
) = PendingIntent.getActivity(
    context,
    0,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE),
)
