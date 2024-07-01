package au.com.shiftyjelly.pocketcasts.clip

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareClipFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val clipColors get() = ClipColors(args.baseColor)

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
        val clipColors = clipColors
        setContent {
            val state by viewModel.uiState.collectAsState()

            ShareClipPage(
                episode = state.episode,
                podcast = state.podcast,
                clipRange = state.clipRange,
                playbackProgress = state.playbackProgress,
                episodeCount = state.episodeCount,
                isPlaying = state.isPlaying,
                useEpisodeArtwork = state.useEpisodeArtwork,
                clipColors = clipColors,
                onPlayClick = viewModel::playClip,
                onPauseClick = viewModel::pauseClip,
                onClip = {
                    state.podcast?.let { podcast ->
                        state.clip?.let { clip ->
                            shareClip(podcast, clip)
                        }
                    }
                },
                onClipStartUpdate = viewModel::updateClipStart,
                onClipEndUpdate = viewModel::updateClipEnd,
                onPlaybackProgressUpdate = viewModel::onClipProgressUpdate,
                onTimelineScaleUpdate = viewModel::onTimelineResolutionUpdate,
                onClose = ::dismiss,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clipColors = clipColors
        val argbColor = clipColors.background.toArgb()

        requireActivity().window?.let { activityWindow ->
            activityWindow.statusBarColor = argbColor
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = clipColors.background.luminance() > 0.5f
        }
        requireDialog().window?.let { dialogWindow ->
            dialogWindow.navigationBarColor = argbColor
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = clipColors.background.luminance() > 0.5f
        }
        bottomSheetView()?.backgroundTintList = ColorStateList.valueOf(argbColor)
    }

    private fun shareClip(podcast: Podcast, clip: Clip) {
        viewModel.onClipLinkShared(clip)
        SharePodcastHelper(
            podcast,
            clip.episode,
            clip.range.start,
            clip.range.end,
            requireActivity(),
            SharePodcastHelper.ShareType.CLIP,
            SourceView.CLIP_SHARING,
            clipAnalytics.analyticsTracker,
        ).showShareDialogDirect()
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        val clipUuid: String,
        val clipRange: Clip.Range,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareClipFragmentArgs"

        fun newInstance(
            episode: PodcastEpisode,
            @ColorInt baseColor: Int,
            source: SourceView,
        ) = ShareClipFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    episodeUuid = episode.uuid,
                    podcastUuid = episode.podcastUuid,
                    clipUuid = UUID.randomUUID().toString(),
                    clipRange = Clip.Range.fromPosition(episode.playedUpTo.seconds, episode.duration.seconds),
                    baseColor = Color(baseColor),
                    source = source,
                ),
            )
        }
    }
}
