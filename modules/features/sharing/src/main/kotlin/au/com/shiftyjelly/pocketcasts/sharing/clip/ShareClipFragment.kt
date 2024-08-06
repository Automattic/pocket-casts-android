package au.com.shiftyjelly.pocketcasts.sharing.clip

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.utils.parceler.DurationParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareClipFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val shareColors get() = ShareColors(args.baseColor)

    private val viewModel by viewModels<ShareClipViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareClipViewModel.Factory> { factory ->
                factory.create(
                    args.episodeUuid,
                    args.clipRange,
                    clipPlayerFactory.create(requireActivity().applicationContext),
                    clipAnalytics,
                )
            }
        },
    )

    @Inject
    lateinit var clipPlayerFactory: ClipPlayer.Factory

    @Inject
    lateinit var sharingClient: SharingClient

    @Inject
    lateinit var clipAnalyticsFactory: ClipAnalytics.Factory

    private lateinit var clipAnalytics: ClipAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipAnalytics = clipAnalyticsFactory.create(
            episodeId = args.episodeUuid,
            podcastId = args.podcastUuid,
            clipId = args.clipUuid,
            sourceView = args.source,
            initialClipRange = args.clipRange,
        )
        if (savedInstanceState == null) {
            viewModel.onClipScreenShown()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val shareColors = shareColors
        val assetController = BackgroundAssetController.create(requireContext(), shareColors)
        val listener = ShareClipListener(this@ShareClipFragment, viewModel, sharingClient, assetController)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            ShareClipPage(
                episode = uiState.episode,
                podcast = uiState.podcast,
                clipRange = uiState.clipRange,
                playbackProgress = uiState.playbackProgress,
                isPlaying = uiState.isPlaying,
                useEpisodeArtwork = uiState.useEpisodeArtwork,
                shareColors = shareColors,
                assetController = assetController,
                listener = listener,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        styleBackgroundColor(shareColors.background.toArgb())
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        val clipUuid: String,
        @TypeParceler<Duration, DurationParceler>() val clipStart: Duration,
        @TypeParceler<Duration, DurationParceler>() val clipEnd: Duration,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
    ) : Parcelable {
        val clipRange get() = Clip.Range(clipStart, clipEnd)
    }

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareClipFragmentArgs"

        fun newInstance(
            episode: PodcastEpisode,
            @ColorInt baseColor: Int,
            source: SourceView,
        ) = ShareClipFragment().apply {
            val clipRange = Clip.Range.fromPosition(
                playbackPosition = episode.playedUpTo.seconds,
                episodeDuration = episode.duration.seconds,
            )

            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    episodeUuid = episode.uuid,
                    podcastUuid = episode.podcastUuid,
                    clipUuid = UUID.randomUUID().toString(),
                    clipStart = clipRange.start,
                    clipEnd = clipRange.end,
                    baseColor = Color(baseColor),
                    source = source,
                ),
            )
        }
    }
}
