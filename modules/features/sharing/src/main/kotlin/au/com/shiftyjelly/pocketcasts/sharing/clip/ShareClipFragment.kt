package au.com.shiftyjelly.pocketcasts.sharing.clip

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
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.ShareActions
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
    lateinit var clipAnalyticsFactory: ClipAnalytics.Factory

    private lateinit var clipAnalytics: ClipAnalytics

    @Inject
    lateinit var shareActionsFactory: ShareActions.Factory

    private lateinit var shareActions: ShareActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipAnalytics = clipAnalyticsFactory.create(
            episodeId = args.episodeUuid,
            podcastId = args.podcastUuid,
            clipId = args.clipUuid,
            sourceView = args.source,
            initialClipRange = args.clipRange,
        )
        shareActions = shareActionsFactory.create(args.source)
        if (savedInstanceState == null) {
            viewModel.onClipScreenShown()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val listener = ShareClipViewModelListener(this@ShareClipFragment, viewModel, shareActions)
        val shareColors = shareColors
        setContent {
            val state by viewModel.uiState.collectAsState()

            ShareClipPage(
                episode = state.episode,
                podcast = state.podcast,
                clipRange = state.clipRange,
                playbackProgress = state.playbackProgress,
                isPlaying = state.isPlaying,
                useEpisodeArtwork = state.useEpisodeArtwork,
                shareColors = shareColors,
                listener = listener,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val shareColors = shareColors
        val argbColor = shareColors.background.toArgb()

        requireActivity().window?.let { activityWindow ->
            activityWindow.statusBarColor = argbColor
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = shareColors.background.luminance() > 0.5f
        }
        requireDialog().window?.let { dialogWindow ->
            dialogWindow.navigationBarColor = argbColor
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = shareColors.background.luminance() > 0.5f
        }
        bottomSheetView()?.backgroundTintList = ColorStateList.valueOf(argbColor)
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
