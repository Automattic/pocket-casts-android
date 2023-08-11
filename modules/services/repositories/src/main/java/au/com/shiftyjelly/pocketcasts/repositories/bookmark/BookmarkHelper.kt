package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.INTENT_OPEN_APP_ADD_BOOKMARK
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.INTENT_OPEN_APP_VIEW_BOOKMARKS
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.isAppForeground
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarkHelper @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val bookmarkManager: BookmarkManager,
    private val settings: Settings,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun handleAddBookmarkAction(
        context: Context,
    ) {
        if (context.isAppForeground()) {
            val bookmarkIntent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.apply { action = INTENT_OPEN_APP_ADD_BOOKMARK }
            bookmarkIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(bookmarkIntent)
        } else {
            if (playbackManager.getCurrentEpisode() == null) return
            launch {
                val episode = playbackManager.getCurrentEpisode() ?: return@launch
                val timeInSecs = playbackManager.getCurrentTimeMs(episode) / 1000

                // Load existing bookmark
                val bookmark = bookmarkManager.findByEpisodeTime(
                    episode = episode,
                    timeSecs = timeInSecs
                )

                if (bookmark == null) {
                    bookmarkManager.add(
                        episode = episode,
                        timeSecs = timeInSecs,
                        title = context.getString(LR.string.bookmark)
                    )
                }

                if (settings.getHeadphoneControlsPlayBookmarkConfirmationSound()) {
                    playbackManager.playTone()
                }

                buildAndShowNotification(context)
            }
        }
    }
}

private fun buildAndShowNotification(
    context: Context,
) {
    val changeTitleAction = NotificationCompat.Action(
        IR.drawable.ic_notification_edit,
        context.getString(LR.string.bookmark_notification_action_change_title),
        buildPendingIntent(context, INTENT_OPEN_APP_ADD_BOOKMARK)
    )

    val notification = NotificationCompat.Builder(
        context,
        Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id
    )
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentTitle(context.getString(LR.string.bookmark_notification_title_added))
        .setContentText(context.getString(LR.string.bookmark_notification_content_tap_to_view))
        .setSmallIcon(IR.drawable.notification)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(buildPendingIntent(context, INTENT_OPEN_APP_VIEW_BOOKMARKS))
        .addAction(changeTitleAction)
        .build()
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        NotificationManagerCompat.from(context)
            .notify(Settings.NotificationId.BOOKMARK.value, notification)
    } else {
        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Post notification permission not granted.")
    }
}

private fun buildPendingIntent(context: Context, actionKey: String): PendingIntent {
    val appIntent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.apply { action = actionKey }

    return PendingIntent.getActivity(
        context,
        0,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE)
    )
}
