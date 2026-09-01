package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val INTENT_ACTION_REFRESH_PODCASTS = "au.com.shiftyjelly.pocketcasts.action.REFRESH_PODCASTS"
        const val INTENT_ACTION_NOTIFICATION_PLAY = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_PLAY"
        const val INTENT_ACTION_NOTIFICATION_PAUSE = "au.com.shiftyjelly.pocketcasts.action.NOTIFICATION_PAUSE"
        const val INTENT_ACTION_WIDGET_PLAY = "au.com.shiftyjelly.pocketcasts.action.WIDGET_PLAY"
        const val INTENT_ACTION_WIDGET_PAUSE = "au.com.shiftyjelly.pocketcasts.action.WIDGET_PAUSE"
        const val INTENT_ACTION_SKIP_FORWARD = "au.com.shiftyjelly.pocketcasts.action.SKIP_FORWARD"
        const val INTENT_ACTION_SKIP_BACKWARD = "au.com.shiftyjelly.pocketcasts.action.SKIP_BACKWARD"
        const val INTENT_ACTION_PLAY = "au.com.shiftyjelly.pocketcasts.action.PLAY"
        const val INTENT_ACTION_PAUSE = "au.com.shiftyjelly.pocketcasts.action.PAUSE"
        const val INTENT_ACTION_STOP = "au.com.shiftyjelly.pocketcasts.action.STOP"
        const val INTENT_ACTION_NEXT = "au.com.shiftyjelly.pocketcasts.action.NEXT"

        // Optional extra on the skip actions to override the user's configured skip amount.
        const val INTENT_EXTRA_SECONDS = "au.com.shiftyjelly.pocketcasts.extra.SECONDS"
    }

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    private val sourceView = SourceView.PLAYER_BROADCAST_ACTION

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == INTENT_ACTION_REFRESH_PODCASTS) {
            podcastManager.refreshPodcasts("broadcast")
        } else {
            when (intent.action) {
                INTENT_ACTION_NOTIFICATION_PLAY, INTENT_ACTION_WIDGET_PLAY, INTENT_ACTION_PLAY -> play()
                INTENT_ACTION_NOTIFICATION_PAUSE, INTENT_ACTION_WIDGET_PAUSE, INTENT_ACTION_PAUSE -> pause()
                INTENT_ACTION_STOP -> stop()
                INTENT_ACTION_NEXT -> playNext()
                INTENT_ACTION_SKIP_FORWARD -> skipForward(intent.skipAmountSecondsOrNull())
                INTENT_ACTION_SKIP_BACKWARD -> skipBackward(intent.skipAmountSecondsOrNull())
            }
            // To help us with debugging user support emails log where the user took the action.
            val logFrom = when (intent.action) {
                INTENT_ACTION_NOTIFICATION_PLAY, INTENT_ACTION_NOTIFICATION_PAUSE -> "notification"
                INTENT_ACTION_WIDGET_PLAY, INTENT_ACTION_WIDGET_PAUSE -> "widget"
                else -> "external"
            }
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Intent from %s. %s", logFrom, intent.action ?: "")
        }
    }

    private fun skipBackward(jumpAmountSeconds: Int?) {
        playbackManager.skipBackward(
            sourceView = sourceView,
            jumpAmountSeconds = jumpAmountSeconds ?: settings.skipBackInSecs.value,
        )
    }

    private fun skipForward(jumpAmountSeconds: Int?) {
        playbackManager.skipForward(
            sourceView = sourceView,
            jumpAmountSeconds = jumpAmountSeconds ?: settings.skipForwardInSecs.value,
        )
    }

    private fun pause() {
        playbackManager.pause(sourceView = sourceView)
    }

    private fun play() {
        playbackManager.playQueue(sourceView = sourceView)
    }

    private fun playNext() {
        playbackManager.playNextInQueue(sourceView = sourceView)
    }

    private fun stop() {
        playbackManager.stopAsync(sourceView = sourceView)
    }
}

/**
 * Reads the skip amount override, or null to fall back to the user's setting.
 */
internal fun Intent.skipAmountSecondsOrNull(): Int? {
    val seconds = getStringExtra(PlayerBroadcastReceiver.INTENT_EXTRA_SECONDS)?.toIntOrNull()
        ?: getIntExtra(PlayerBroadcastReceiver.INTENT_EXTRA_SECONDS, 0)
    // Capped because PlaybackManager converts the amount to milliseconds in Int arithmetic.
    return seconds.takeIf { it > 0 }?.coerceAtMost(MAX_SKIP_AMOUNT_SECONDS)
}

private const val MAX_SKIP_AMOUNT_SECONDS = 86_400
