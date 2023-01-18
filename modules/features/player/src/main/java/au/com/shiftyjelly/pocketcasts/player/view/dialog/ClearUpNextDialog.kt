package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * A dialog that shows a clear Up Next confirmation dialog if there are more than two episodes in the queue.
 */
@AndroidEntryPoint
class ClearUpNextDialog(
    private val source: UpNextSource = UpNextSource.UNKNOWN,
    private val removeNowPlaying: Boolean,
    private val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    context: Context
) : ConfirmationDialog() {

    init {
        setTitle(context.getString(LR.string.player_up_next_clear_queue_button))
        setSummary(context.getString(LR.string.player_up_next_clear_queue_summary))
        setIconId(R.drawable.ic_upnext_remove)
        val episodeCount = playbackManager.upNextQueue.queueEpisodes.size
        setButtonType(ButtonType.Danger(context.getString(LR.string.player_up_next_clear_episodes_plural, episodeCount)))
        setOnConfirm { clear() }
    }

    private fun clear() {
        analyticsTracker.track(AnalyticsEvent.UP_NEXT_QUEUE_CLEARED, mapOf(SOURCE_KEY to source.analyticsValue))
        if (removeNowPlaying) {
            playbackManager.endPlaybackAndClearUpNextAsync()
        } else {
            playbackManager.clearUpNextAsync()
        }
    }

    fun showOrClear(fragmentManager: FragmentManager) {
        val episodeCount = playbackManager.upNextQueue.queueEpisodes.size
        return if (episodeCount >= 3) {
            show(fragmentManager, "mini_player_clear_dialog")
        } else {
            clear()
        }
    }

    companion object {
        private const val SOURCE_KEY = "source"
    }
}
