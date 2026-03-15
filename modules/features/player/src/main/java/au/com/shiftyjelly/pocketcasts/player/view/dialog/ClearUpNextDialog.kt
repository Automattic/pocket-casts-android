package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpNextQueueClearedEvent
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * A dialog that shows a clear Up Next confirmation dialog if there are more than two episodes in the queue.
 */
@AndroidEntryPoint
class ClearUpNextDialog(
    private val source: UpNextSource,
    private val removeNowPlaying: Boolean,
    private val playbackManager: PlaybackManager,
    private val eventHorizon: EventHorizon,
    context: Context,
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
        eventHorizon.track(
            UpNextQueueClearedEvent(
                source = source.eventHorizonValue,
            ),
        )
        if (removeNowPlaying) {
            playbackManager.endPlaybackAndClearUpNextAsync()
        } else {
            playbackManager.clearUpNextAsync()
        }
    }

    fun showOrClear(fragmentManager: FragmentManager, tag: String) {
        val episodeCount = playbackManager.upNextQueue.queueEpisodes.size
        return if (episodeCount >= 3) {
            showClearUpNextConfirmationDialog(fragmentManager, tag)
        } else {
            clear()
        }
    }

    fun showClearUpNextConfirmationDialog(fragmentManager: FragmentManager, tag: String) = show(fragmentManager, tag)
}
