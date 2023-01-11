package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SleepTimerReceiver : BroadcastReceiver() {

    @Inject lateinit var playbackManager: PlaybackManager

    override fun onReceive(context: Context, intent: Intent) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused from sleep timer.")
        Toast.makeText(context, "Sleep timer stopped your podcast.\nNight night!", Toast.LENGTH_LONG).show()
        playbackManager.pause(playbackSource = AnalyticsSource.AUTO_PAUSE)
        playbackManager.updateSleepTimerStatus(running = false)
    }
}
