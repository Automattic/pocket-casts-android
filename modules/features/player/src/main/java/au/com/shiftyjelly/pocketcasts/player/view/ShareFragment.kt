package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShareBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper.ShareType
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@AndroidEntryPoint
class ShareFragment : BaseDialogFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    override val statusBarColor: StatusBarColor? = null

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentShareBinding? = null
    private var disposable: Disposable? = null

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentShareBinding.inflate(inflater, container, false)
        this.binding = binding

        val podcast = viewModel.podcast
        val episode = viewModel.episode

        binding.buttonSharePodcast.setOnClickListener {
            if (podcast != null) {
                SharePodcastHelper(
                    podcast,
                    null,
                    null,
                    requireContext(),
                    ShareType.PODCAST,
                    AnalyticsSource.PLAYER,
                    analyticsTracker
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonShareEpisode.setOnClickListener {
            if (podcast != null && episode is Episode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    null,
                    requireContext(),
                    ShareType.EPISODE,
                    AnalyticsSource.PLAYER,
                    analyticsTracker
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonShareCurrentPosition.setOnClickListener {
            if (podcast != null && episode is Episode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    episode.playedUpTo,
                    requireContext(),
                    ShareType.CURRENT_TIME,
                    AnalyticsSource.PLAYER,
                    analyticsTracker
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonOpenFileIn.setOnClickListener {
            if (podcast != null && episode is Episode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    episode.playedUpTo,
                    requireContext(),
                    ShareType.EPISODE_FILE,
                    AnalyticsSource.PLAYER,
                    analyticsTracker
                ).sendFile()
            }
            close()
        }

        binding.sharePodcast.isVisible = podcast != null
        binding.shareEpisode.isVisible = episode != null
        binding.shareCurrentPosition.isVisible = episode != null
        binding.openFileIn.isVisible = episode != null && episode.isDownloaded

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            applyColor(theme, backgroundColor)
        }

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun close() {
        dismiss()
    }
}
