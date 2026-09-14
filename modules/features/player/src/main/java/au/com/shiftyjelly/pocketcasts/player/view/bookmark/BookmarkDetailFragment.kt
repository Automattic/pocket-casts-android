package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.reimagine.timestamp.ShareEpisodeTimestampFragment
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog.ButtonType.Danger
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.BookmarkPlayTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class BookmarkDetailFragment : BaseDialogFragment() {

    companion object {
        private const val LISTENER_TAKEOVER_TOLERANCE_MS = 1000
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
                        bookmarkUuid = bookmark.uuid,
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
        val bookmarkUuid: String,
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
    internal lateinit var podcastManager: PodcastManager

    @Inject
    internal lateinit var bookmarkManager: BookmarkManager

    @Inject
    internal lateinit var eventHorizon: EventHorizon

    @Inject
    internal lateinit var bookmarkPlaybackTimeResolver: BookmarkPlaybackTimeResolver

    private val isResolving = MutableStateFlow(false)

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    private val viewModel: BookmarkDetailViewModel by viewModels()

    private val editBookmarkLauncher = registerForActivityResult(BookmarkActivityContract()) { result ->
        if (result != null) {
            viewModel.refresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val hasTranscript = args.passage != null && FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)
        LaunchedEffect(Unit) {
            viewModel.load(
                bookmarkUuid = args.bookmarkUuid,
                title = args.title,
                episodeUuid = args.episodeUuid,
                podcastUuid = args.podcastUuid,
                podcastTitle = args.podcastTitle,
                passage = args.passage,
                passageLocation = args.passageLocation,
            )
        }
        DialogBox(fillMaxHeight = hasTranscript) {
            val resolving by isResolving.collectAsState()
            val uiState by viewModel.uiState.collectAsState()
            BookmarkDetailPage(
                title = uiState.title,
                episodeTitle = args.episodeTitle,
                podcastTitle = uiState.podcastTitle,
                timeSecs = args.timeSecs,
                createdAtText = args.createdAtText,
                isResolving = resolving,
                onPlayClick = ::onPlayClick,
                onClose = { dismiss() },
                onArtworkClick = ::onArtworkClick,
                onMoreClick = ::onMoreClick,
                episode = uiState.episode,
                useEpisodeArtwork = uiState.useEpisodeArtwork,
                isPodcastTitleLoading = uiState.isPodcastTitleLoading,
                passage = uiState.passage,
                transcriptState = uiState.transcriptState,
            )
        }
    }

    private fun onArtworkClick() {
        if (args.sourceView == SourceView.EPISODE_DETAILS) {
            dismiss()
            return
        }
        (activity as? FragmentHostListener)?.openEpisodeDialog(
            episodeUuid = args.episodeUuid,
            source = EpisodeViewSource.UNKNOWN,
            podcastUuid = args.podcastUuid,
            forceDark = args.sourceView == SourceView.PLAYER,
            autoPlay = false,
        )
    }

    private fun onMoreClick() {
        val fragmentManager = activity?.supportFragmentManager ?: return
        lifecycleScope.launch {
            val episode = episodeManager.findEpisodeByUuid(args.episodeUuid)
            val dialog = OptionsDialog()
                .setForceDarkTheme(args.sourceView == SourceView.PLAYER)
                .addTextOption(
                    titleId = LR.string.edit,
                    imageId = IR.drawable.ic_edit,
                    click = ::onEditClick,
                )
            if (episode is PodcastEpisode) {
                dialog.addTextOption(
                    titleId = LR.string.share,
                    imageId = IR.drawable.ic_share,
                    click = { onShareClick(episode) },
                )
            }
            dialog.addTextOption(
                titleId = LR.string.bookmarks_delete_singular,
                imageId = IR.drawable.ic_delete,
                click = ::onDeleteClick,
            )
            dialog.show(fragmentManager, "bookmark_detail_options")
        }
    }

    private fun onShareClick(episode: PodcastEpisode) {
        lifecycleScope.launch {
            val podcast = podcastManager.findPodcastByUuid(args.podcastUuid) ?: return@launch
            ShareEpisodeTimestampFragment
                .forBookmark(episode, args.timeSecs.seconds, podcast.backgroundColor, args.sourceView)
                .show(parentFragmentManager, "share_screen")
        }
    }

    private fun onDeleteClick() {
        val fragmentManager = activity?.supportFragmentManager ?: return
        ConfirmationDialog()
            .setForceDarkTheme(args.sourceView == SourceView.PLAYER)
            .setTitle(getString(LR.string.bookmarks_delete_singular))
            .setSummary(getString(LR.string.bookmarks_delete_summary_singular))
            .setIconId(IR.drawable.ic_delete)
            .setButtonType(Danger(getString(LR.string.delete)))
            .setOnConfirm { onDeleteConfirmed() }
            .show(fragmentManager, "bookmark_detail_delete")
    }

    private fun onDeleteConfirmed() {
        lifecycleScope.launch {
            bookmarkManager.deleteToSync(args.bookmarkUuid)
            dismiss()
        }
    }

    private fun onEditClick() {
        lifecycleScope.launch {
            val podcast = podcastManager.findPodcastByUuid(args.podcastUuid)
            val arguments = BookmarkArguments(
                bookmarkUuid = args.bookmarkUuid,
                episodeUuid = args.episodeUuid,
                timeSecs = args.timeSecs,
                podcastColors = podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode,
            )
            editBookmarkLauncher.launch(BookmarkActivity.launchIntent(requireContext(), arguments))
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
            val positionBeforeResolveMs = if (pausedForResolve) {
                playbackManager.getCurrentTimeMs(episode)
            } else {
                0
            }
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
            if (pausedForResolve && listenerTookOver(episode, positionBeforeResolveMs)) {
                return@launch
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
        }
    }

    private suspend fun listenerTookOver(episode: BaseEpisode, positionBeforeResolveMs: Int): Boolean {
        if (playbackManager.getCurrentEpisode()?.uuid != episode.uuid) return true
        if (playbackManager.isPlaying()) return true
        return abs(playbackManager.getCurrentTimeMs(episode) - positionBeforeResolveMs) >= LISTENER_TAKEOVER_TOLERANCE_MS
    }
}
