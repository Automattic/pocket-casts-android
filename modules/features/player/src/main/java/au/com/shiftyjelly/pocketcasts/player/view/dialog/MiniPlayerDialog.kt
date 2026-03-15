package au.com.shiftyjelly.pocketcasts.player.view.dialog

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MiniPlayerLongPressMenuDismissedEvent
import com.automattic.eventhorizon.MiniPlayerLongPressMenuOptionTappedEvent
import com.automattic.eventhorizon.MiniPlayerLongPressMenuShownEvent
import com.automattic.eventhorizon.MiniPlayerModalOptionType
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
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
) {
    private var isOptionClicked = false
    fun show(context: Context) {
        eventHorizon.track(MiniPlayerLongPressMenuShownEvent)
        val dangerColor = context.getThemeColor(UR.attr.support_05)
        OptionsDialog()
            .addTextOption(
                titleId = LR.string.mark_played,
                imageId = IR.drawable.ic_markasplayed,
                click = {
                    isOptionClicked = true
                    eventHorizon.track(
                        MiniPlayerLongPressMenuOptionTappedEvent(
                            option = MiniPlayerModalOptionType.MarkPlayed,
                        ),
                    )
                    markAsPlayed()
                },
            )
            .addTextOption(
                titleId = LR.string.player_end_playback_clear_up_next,
                titleColor = dangerColor,
                imageId = IR.drawable.ic_close,
                imageColor = dangerColor,
                click = {
                    isOptionClicked = true
                    eventHorizon.track(
                        MiniPlayerLongPressMenuOptionTappedEvent(
                            option = MiniPlayerModalOptionType.CloseAndClearUpNext,
                        ),
                    )
                    endPlaybackAndClearUpNext(context)
                },
            )
            .setOnDismiss {
                if (!isOptionClicked) {
                    eventHorizon.track(MiniPlayerLongPressMenuDismissedEvent)
                }
            }
            .show(fragmentManager, "mini_player_dialog")
    }

    private fun endPlaybackAndClearUpNext(context: Context) {
        val dialog = ClearUpNextDialog(
            source = UpNextSource.MINI_PLAYER,
            removeNowPlaying = true,
            playbackManager = playbackManager,
            eventHorizon = eventHorizon,
            context = context,
        )
        dialog.showOrClear(fragmentManager, tag = "mini_player_clear_dialog")
    }

    private fun markAsPlayed() {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        episodeManager.markAsPlayedAsync(episode, playbackManager, podcastManager, settings.upNextShuffle.value)
        eventHorizon.track(
            EpisodeMarkedAsPlayedEvent(
                episodeUuid = episode.uuid,
                source = SourceView.MINIPLAYER.eventHorizonValue,
            ),
        )
    }
}
