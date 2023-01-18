package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
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
    }

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var playbackManager: PlaybackManager
    private val playbackSource = AnalyticsSource.PLAYER_BROADCAST_ACTION

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == INTENT_ACTION_REFRESH_PODCASTS) {
            podcastManager.refreshPodcasts("broadcast")
        } else {
            when (intent.action) {
                INTENT_ACTION_NOTIFICATION_PLAY, INTENT_ACTION_WIDGET_PLAY, INTENT_ACTION_PLAY -> play()
                INTENT_ACTION_NOTIFICATION_PAUSE, INTENT_ACTION_WIDGET_PAUSE, INTENT_ACTION_PAUSE -> pause()
                INTENT_ACTION_STOP -> stop()
                INTENT_ACTION_NEXT -> playNext()
                INTENT_ACTION_SKIP_FORWARD -> skipForward()
                INTENT_ACTION_SKIP_BACKWARD -> skipBackward()
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

    private fun skipBackward() {
        playbackManager.skipBackward(playbackSource = playbackSource)
    }

    private fun skipForward() {
        playbackManager.skipForward(playbackSource = playbackSource)
    }

    private fun pause() {
        playbackManager.pause(playbackSource = playbackSource)
    }

    private fun play() {
        playbackManager.playQueue(playbackSource = playbackSource)
    }

    private fun playNext() {
        playbackManager.playNextInQueue(playbackSource = playbackSource)
    }

    private fun stop() {
        playbackManager.stopAsync(playbackSource = playbackSource)
    }
}
