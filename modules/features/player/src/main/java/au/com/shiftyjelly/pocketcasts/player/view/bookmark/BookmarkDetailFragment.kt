package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.BookmarkPlayTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class BookmarkDetailFragment : BaseDialogFragment() {

    companion object {
        private const val TAG = "bookmark_detail"
        private const val NEW_INSTANCE_ARG = "bookmark_detail_args"

        fun show(
            fragmentManager: FragmentManager,
            bookmark: Bookmark,
            episodeTitle: String,
            podcastUuid: String,
            podcastTitle: String,
            sourceView: SourceView,
        ) {
            if (!fragmentManager.isStateSaved && fragmentManager.findFragmentByTag(TAG) == null) {
                newInstance(bookmark, episodeTitle, podcastUuid, podcastTitle, sourceView)
                    .show(fragmentManager, TAG)
            }
        }

        private fun newInstance(
            bookmark: Bookmark,
            episodeTitle: String,
            podcastUuid: String,
            podcastTitle: String,
            sourceView: SourceView,
        ) = BookmarkDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_ARG,
                    Args(
                        displayTitle = bookmark.displayTitle,
                        aiSummary = bookmark.aiSummary,
                        episodeTitle = episodeTitle,
                        episodeUuid = bookmark.episodeUuid,
                        podcastUuid = podcastUuid,
                        podcastTitle = podcastTitle,
                        timeSecs = bookmark.timeSecs,
                        createdAtText = bookmark.createdAt.toLocalizedFormatPattern(bookmark.createdAtDatePattern()),
                        sourceView = sourceView,
                    ),
                )
            }
        }
    }

    @Parcelize
    private data class Args(
        val displayTitle: String,
        val aiSummary: String?,
        val episodeTitle: String,
        val episodeUuid: String,
        val podcastUuid: String,
        val podcastTitle: String,
        val timeSecs: Int,
        val createdAtText: String,
        val sourceView: SourceView,
    ) : Parcelable

    @Inject
    internal lateinit var playbackManager: PlaybackManager

    @Inject
    internal lateinit var episodeManager: EpisodeManager

    @Inject
    internal lateinit var eventHorizon: EventHorizon

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(fillMaxHeight = false) {
            BookmarkDetailPage(
                displayTitle = args.displayTitle,
                aiSummary = args.aiSummary,
                episodeTitle = args.episodeTitle,
                podcastUuid = args.podcastUuid,
                podcastTitle = args.podcastTitle,
                timeSecs = args.timeSecs,
                createdAtText = args.createdAtText,
                onPlayClick = ::onPlayClick,
                onClose = { dismiss() },
            )
        }
    }

    private fun onPlayClick() {
        lifecycleScope.launch {
            val episode = episodeManager.findEpisodeByUuid(args.episodeUuid)
            if (episode == null) {
                Toast.makeText(
                    requireContext(),
                    getString(LR.string.episode_not_found),
                    Toast.LENGTH_SHORT,
                ).show()
                dismiss()
                return@launch
            }
            playbackManager.playNowSuspend(episode, sourceView = args.sourceView)
            playbackManager.seekToTimeMs(positionMs = args.timeSecs * 1000)
            eventHorizon.track(
                BookmarkPlayTappedEvent(
                    source = args.sourceView.analyticsValue,
                    episodeUuid = args.episodeUuid,
                    podcastUuid = args.podcastUuid,
                ),
            )
            dismiss()
        }
    }
}
