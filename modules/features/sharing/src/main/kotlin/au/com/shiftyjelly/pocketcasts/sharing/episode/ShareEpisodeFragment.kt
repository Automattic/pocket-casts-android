package au.com.shiftyjelly.pocketcasts.sharing.episode

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
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareEpisodeFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val shareColors get() = ShareColors(args.baseColor)

    private val viewModel by viewModels<ShareEpisodeViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareEpisodeViewModel.Factory> { factory ->
                factory.create(args.episodeUuid)
            }
        },
    )

    @Inject internal lateinit var shareListenerFactory: ShareEpisodeListener.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val platforms = SocialPlatform.getAvailablePlatforms(requireContext())
        val assetController = BackgroundAssetController.create(requireContext(), shareColors)
        val listener = shareListenerFactory.create(this@ShareEpisodeFragment, args.source)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            ShareEpisodePage(
                podcast = uiState.podcast,
                episode = uiState.episode,
                useEpisodeArtwork = uiState.useEpisodeArtwork,
                socialPlatforms = platforms,
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
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareEpisodeFragmentArgs"

        fun newInstance(
            episode: PodcastEpisode,
            @ColorInt baseColor: Int,
            source: SourceView,
        ) = ShareEpisodeFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    episodeUuid = episode.uuid,
                    baseColor = Color(baseColor),
                    source = source,
                ),
            )
        }
    }
}
