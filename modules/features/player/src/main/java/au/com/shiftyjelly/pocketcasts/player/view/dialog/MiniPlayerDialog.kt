package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

/**
 * A dialog to either mark the current episode as played or clear the Up Next.
 */
class MiniPlayerDialog(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val fragmentManager: FragmentManager
) {

    fun show(context: Context) {
        val dangerColor = context.getThemeColor(UR.attr.support_05)
        OptionsDialog()
            .addTextOption(
                titleId = LR.string.mark_played,
                imageId = IR.drawable.ic_markasplayed,
                click = { markAsPlayed() }
            )
            .addTextOption(
                titleId = LR.string.player_end_playback_clear_up_next,
                titleColor = dangerColor,
                imageId = IR.drawable.ic_close,
                imageColor = dangerColor,
                click = { endPlaybackAndClearUpNext(context) }
            )
            .show(fragmentManager, "mini_player_dialog")
    }

    private fun endPlaybackAndClearUpNext(context: Context) {
        ClearUpNextDialog(removeNowPlaying = true, playbackManager = playbackManager, context = context)
            .showOrClear(fragmentManager)
    }

    private fun markAsPlayed() {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
    }
}
