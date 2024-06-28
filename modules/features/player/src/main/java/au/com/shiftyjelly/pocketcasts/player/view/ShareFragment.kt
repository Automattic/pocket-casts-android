package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.clip.ShareClipFragment
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShareBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper.ShareType
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ShareFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    @Inject lateinit var analyticsTracker: AnalyticsTracker
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
                    null,
                    requireContext(),
                    ShareType.PODCAST,
                    SourceView.PLAYER,
                    analyticsTracker,
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonShareEpisode.setOnClickListener {
            if (podcast != null && episode is PodcastEpisode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    null,
                    null,
                    requireContext(),
                    ShareType.EPISODE,
                    SourceView.PLAYER,
                    analyticsTracker,
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonShareCurrentPosition.setOnClickListener {
            if (podcast != null && episode is PodcastEpisode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    episode.playedUpTo.seconds,
                    null,
                    requireContext(),
                    ShareType.CURRENT_TIME,
                    SourceView.PLAYER,
                    analyticsTracker,
                ).showShareDialogDirect()
            }
            close()
        }
        binding.buttonOpenFileIn.setOnClickListener {
            if (podcast != null && episode is PodcastEpisode) {
                SharePodcastHelper(
                    podcast,
                    episode,
                    episode.playedUpTo.seconds,
                    null,
                    requireContext(),
                    ShareType.EPISODE_FILE,
                    SourceView.PLAYER,
                    analyticsTracker,
                ).sendFile()
            }
            close()
        }
        binding.buttonShareClip.setOnClickListener {
            if (podcast != null && episode is PodcastEpisode) {
                ShareClipFragment
                    .newInstance(episode, podcast.backgroundColor, args.source)
                    .show(parentFragmentManager, "share_clip")
            }
            close()
        }

        binding.sharePodcast.isVisible = podcast != null
        binding.shareEpisode.isVisible = episode != null
        binding.shareCurrentPosition.isVisible = episode != null
        binding.shareClip.isVisible = FeatureFlag.isEnabled(Feature.SHARE_CLIPS) && episode is PodcastEpisode
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

    @Parcelize
    private class Args(
        val source: SourceView,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareFragmentArgs"

        fun newInstance(
            source: SourceView,
        ) = ShareFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(source = source),
            )
        }
    }
}
