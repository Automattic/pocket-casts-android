package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarkDetailFragment : BaseDialogFragment() {

    companion object {
        private const val TAG = "bookmark_detail"
        private const val ARG_DISPLAY_TITLE = "display_title"
        private const val ARG_AI_SUMMARY = "ai_summary"
        private const val ARG_EPISODE_TITLE = "episode_title"
        private const val ARG_EPISODE_UUID = "episode_uuid"
        private const val ARG_PODCAST_UUID = "podcast_uuid"
        private const val ARG_PODCAST_TITLE = "podcast_title"
        private const val ARG_TIME_SECS = "time_secs"
        private const val ARG_CREATED_AT_TEXT = "created_at_text"
        private const val ARG_SOURCE_VIEW = "source_view"

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
                putString(ARG_DISPLAY_TITLE, bookmark.displayTitle)
                putString(ARG_AI_SUMMARY, bookmark.aiSummary)
                putString(ARG_EPISODE_TITLE, episodeTitle)
                putString(ARG_EPISODE_UUID, bookmark.episodeUuid)
                putString(ARG_PODCAST_UUID, podcastUuid)
                putString(ARG_PODCAST_TITLE, podcastTitle)
                putInt(ARG_TIME_SECS, bookmark.timeSecs)
                putString(
                    ARG_CREATED_AT_TEXT,
                    bookmark.createdAt.toLocalizedFormatPattern(bookmark.createdAtDatePattern()),
                )
                putString(ARG_SOURCE_VIEW, sourceView.key)
            }
        }
    }

    @Inject
    internal lateinit var playbackManager: PlaybackManager

    private val displayTitle: String get() = requireArguments().getString(ARG_DISPLAY_TITLE, "")
    private val aiSummary: String? get() = requireArguments().getString(ARG_AI_SUMMARY)
    private val episodeTitle: String get() = requireArguments().getString(ARG_EPISODE_TITLE, "")
    private val episodeUuid: String get() = requireArguments().getString(ARG_EPISODE_UUID, "")
    private val podcastUuid: String get() = requireArguments().getString(ARG_PODCAST_UUID, "")
    private val podcastTitle: String get() = requireArguments().getString(ARG_PODCAST_TITLE, "")
    private val timeSecs: Int get() = requireArguments().getInt(ARG_TIME_SECS)
    private val createdAtText: String get() = requireArguments().getString(ARG_CREATED_AT_TEXT, "")
    private val sourceView: SourceView
        get() = SourceView.fromString(requireArguments().getString(ARG_SOURCE_VIEW))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(fillMaxHeight = false) {
            BookmarkDetailPage(
                displayTitle = displayTitle,
                aiSummary = aiSummary,
                episodeTitle = episodeTitle,
                podcastUuid = podcastUuid,
                podcastTitle = podcastTitle,
                timeSecs = timeSecs,
                createdAtText = createdAtText,
                onPlayClick = {
                    lifecycleScope.launch {
                        playbackManager.playNowSuspend(
                            episodeUuid = episodeUuid,
                            sourceView = sourceView,
                        )
                        playbackManager.seekToTimeMs(positionMs = timeSecs * 1000)
                    }
                    dismiss()
                },
                onClose = { dismiss() },
            )
        }
    }
}
