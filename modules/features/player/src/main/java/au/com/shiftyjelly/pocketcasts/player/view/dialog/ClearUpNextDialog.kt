package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * A dialog that shows a clear Up Next confirmation dialog if there are more than two episodes in the queue.
 */
class ClearUpNextDialog(private val removeNowPlaying: Boolean, private val playbackManager: PlaybackManager, context: Context) : ConfirmationDialog() {

    init {
        setTitle(context.getString(LR.string.player_up_next_clear_queue_button))
        setSummary(context.getString(LR.string.player_up_next_clear_queue_summary))
        setIconId(R.drawable.ic_upnext_remove)
        val episodeCount = playbackManager.upNextQueue.queueEpisodes.size
        setButtonType(ButtonType.Danger(context.getString(LR.string.player_up_next_clear_episodes_plural, episodeCount)))
        setOnConfirm { clear() }
    }

    private fun clear() {
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
}
