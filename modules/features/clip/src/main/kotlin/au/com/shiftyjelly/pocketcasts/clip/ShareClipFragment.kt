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
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareClipFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val clipColors get() = ClipColors(args.baseColor)

    private val viewModel by viewModels<ShareClipViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareClipViewModel.Factory> { factory ->
                factory.create(args.episodeUuid, clipPlayerFactory.create(requireActivity().applicationContext))
            }
        },
    )

    @Inject
    lateinit var clipPlayerFactory: ClipPlayer.Factory

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

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
                episodeCount = state.episodeCount,
                isPlaying = state.isPlaying,
                useEpisodeArtwork = state.useEpisodeArtwork,
                clipColors = clipColors,
                onPlayClick = viewModel::playClip,
                onPauseClick = viewModel::stopClip,
                onClip = {
                    state.podcast?.let { podcast ->
                        state.clip?.let { clip ->
                            shareClip(podcast, clip)
                        }
                    }
                },
                onClipStartUpdate = viewModel::updateClipStart,
                onClipEndUpdate = viewModel::updateClipEnd,
                onClose = { dismiss() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clipColors = clipColors
        val argbColor = clipColors.backgroundColor.toArgb()

        requireActivity().window?.let { activityWindow ->
            activityWindow.statusBarColor = argbColor
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = clipColors.backgroundColor.luminance() > 0.5f
        }
        requireDialog().window?.let { dialogWindow ->
            dialogWindow.navigationBarColor = argbColor
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = clipColors.backgroundColor.luminance() > 0.5f
        }
        bottomSheetView()?.backgroundTintList = ColorStateList.valueOf(argbColor)
    }

    private fun shareClip(podcast: Podcast, clip: Clip) {
        SharePodcastHelper(
            podcast,
            clip.episode,
            clip.range.start,
            clip.range.end,
            requireActivity(),
            SharePodcastHelper.ShareType.CLIP,
            SourceView.UNKNOWN,
            analyticsTracker,
        ).showShareDialogDirect()
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareClipFragmentArgs"

        fun newInstance(
            episodeUuid: String,
            @ColorInt baseColor: Int,
        ) = ShareClipFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARG to Args(episodeUuid, Color(baseColor)))
        }
    }
}
