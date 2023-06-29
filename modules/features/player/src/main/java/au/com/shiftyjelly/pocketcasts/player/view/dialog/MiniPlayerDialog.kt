package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
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
    private val fragmentManager: FragmentManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
) {
    private var isOptionClicked = false
    fun show(context: Context) {
        analyticsTracker.track(AnalyticsEvent.MINI_PLAYER_LONG_PRESS_MENU_SHOWN)
        val dangerColor = context.getThemeColor(UR.attr.support_05)
        OptionsDialog()
            .addTextOption(
                titleId = LR.string.mark_played,
                imageId = IR.drawable.ic_markasplayed,
                click = {
                    isOptionClicked = true
                    analyticsTracker.track(AnalyticsEvent.MINI_PLAYER_LONG_PRESS_MENU_OPTION_TAPPED, mapOf(OPTION_KEY to MARK_PLAYED))
                    markAsPlayed()
                }
            )
            .addTextOption(
                titleId = LR.string.player_end_playback_clear_up_next,
                titleColor = dangerColor,
                imageId = IR.drawable.ic_close,
                imageColor = dangerColor,
                click = {
                    isOptionClicked = true
                    analyticsTracker.track(AnalyticsEvent.MINI_PLAYER_LONG_PRESS_MENU_OPTION_TAPPED, mapOf(OPTION_KEY to CLOSE_AND_CLEAR_UP_NEXT))
                    endPlaybackAndClearUpNext(context)
                }
            )
            .setOnDismiss {
                if (!isOptionClicked) {
                    analyticsTracker.track(AnalyticsEvent.MINI_PLAYER_LONG_PRESS_MENU_DISMISSED)
                }
            }
            .show(fragmentManager, "mini_player_dialog")
    }

    private fun endPlaybackAndClearUpNext(context: Context) {
        val dialog = ClearUpNextDialog(
            source = UpNextSource.MINI_PLAYER,
            removeNowPlaying = true,
            playbackManager = playbackManager,
            analyticsTracker = analyticsTracker,
            context = context
        )
        dialog.showOrClear(fragmentManager)
    }

    private fun markAsPlayed() {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        episodeManager.markAsPlayedAsync(episode, playbackManager, podcastManager)
        episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, SourceView.MINIPLAYER, episode.uuid)
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val MARK_PLAYED = "mark_played"
        private const val CLOSE_AND_CLEAR_UP_NEXT = "close_and_clear_up_next"
    }
}
