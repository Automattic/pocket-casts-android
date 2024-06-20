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
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
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

    private val viewModel by viewModels<ShareClipViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareClipViewModel.Factory> { factory ->
                factory.create(args.episodeUuid, clipPlayerFactory.create(requireActivity().applicationContext))
            }
        },
    )

    @Inject
    lateinit var clipPlayerFactory: ClipPlayer.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        setContent {
            val state by viewModel.uiState.collectAsState()

            ShareClipPage(
                episode = state.episode,
                isPlaying = state.isPlaying,
                podcastTitle = state.podcastTitle,
                useEpisodeArtwork = state.useEpisodeArtwork,
                baseColor = args.baseColor,
                onPlayClick = { viewModel.playClip() },
                onPauseClick = { viewModel.stopClip() },
                onClose = { dismiss() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backgroundColor = ColorUtils.changeHsvValue(args.baseColor, factor = 0.4f)
        val argbColor = backgroundColor.toArgb()

        requireActivity().window?.let { activityWindow ->
            activityWindow.statusBarColor = argbColor
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = backgroundColor.luminance() > 0.5f
        }
        requireDialog().window?.let { dialogWindow ->
            dialogWindow.navigationBarColor = argbColor
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = backgroundColor.luminance() > 0.5f
        }
        bottomSheetView()?.backgroundTintList = ColorStateList.valueOf(argbColor)
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
