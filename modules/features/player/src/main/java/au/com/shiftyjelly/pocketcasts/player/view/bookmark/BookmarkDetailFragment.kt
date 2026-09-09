package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        title = bookmark.title,
                        referenceTime = bookmark.referenceTime,
                        episodeTitle = episodeTitle,
                        episodeUuid = bookmark.episodeUuid,
                        podcastUuid = podcastUuid,
                        podcastTitle = podcastTitle,
                        timeSecs = bookmark.timeSecs,
                        createdAtText = bookmark.createdAt.toLocalizedFormatPattern(bookmark.createdAtDatePattern()),
                        sourceView = sourceView,
                        passage = bookmark.passage,
                        passageLocation = bookmark.passageLocation,
                    ),
                )
            }
        }
    }

    @Parcelize
    private data class Args(
        val title: String,
        val referenceTime: Int?,
        val episodeTitle: String,
        val episodeUuid: String,
        val podcastUuid: String,
        val podcastTitle: String,
        val timeSecs: Int,
        val createdAtText: String,
        val sourceView: SourceView,
        val passage: String?,
        val passageLocation: Int?,
    ) : Parcelable

    @Inject
    internal lateinit var playbackManager: PlaybackManager

    @Inject
    internal lateinit var episodeManager: EpisodeManager

    @Inject
    internal lateinit var eventHorizon: EventHorizon

    @Inject
    internal lateinit var bookmarkPlaybackTimeResolver: BookmarkPlaybackTimeResolver

    private val isResolving = MutableStateFlow(false)

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(fillMaxHeight = false) {
            val resolving by isResolving.collectAsState()
            BookmarkDetailPage(
                title = args.title,
                episodeTitle = args.episodeTitle,
                podcastUuid = args.podcastUuid,
                podcastTitle = args.podcastTitle,
                timeSecs = args.timeSecs,
                createdAtText = args.createdAtText,
                isResolving = resolving,
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
            val hasReferenceTime = args.referenceTime != null
            val pausedForResolve = hasReferenceTime &&
                playbackManager.isPlaying() &&
                playbackManager.getCurrentEpisode()?.uuid == args.episodeUuid
            if (pausedForResolve) {
                playbackManager.pauseSuspend()
            }
            if (hasReferenceTime) {
                isResolving.value = true
            }
            val seekToMs = try {
                bookmarkPlaybackTimeResolver.playbackTimeMs(
                    episode = episode,
                    referenceTimeSecs = args.referenceTime,
                    fallbackTimeSecs = args.timeSecs,
                )
            } catch (e: CancellationException) {
                if (pausedForResolve) {
                    withContext(NonCancellable) {
                        val stillOurEpisode = playbackManager.getCurrentEpisode()?.uuid == args.episodeUuid
                        if (stillOurEpisode && !playbackManager.isPlaying()) {
                            playbackManager.playNowSuspend(episode, sourceView = args.sourceView)
                        }
                    }
                }
                throw e
            } finally {
                isResolving.value = false
            }
            playbackManager.playNowSuspend(episode, sourceView = args.sourceView)
            playbackManager.seekToTimeMs(positionMs = seekToMs)
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
