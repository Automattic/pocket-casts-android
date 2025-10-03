package au.com.shiftyjelly.pocketcasts.reimagine.episode

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.reimagine.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.reimagine.ui.rememberBackgroundAssetControler
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareEpisodeFragment : BaseDialogFragment() {
    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    private val shareColors get() = ShareColors(args.baseColor)

    private val viewModel by viewModels<ShareEpisodeViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareEpisodeViewModel.Factory> { factory ->
                factory.create(
                    podcastUuid = args.podcastUuid,
                    episodeUuid = args.episodeUuid,
                    sourceView = args.source,
                )
            }
        },
    )

    @Inject internal lateinit var shareListenerFactory: ShareEpisodeListener.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.onScreenShown()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val platforms = remember { SocialPlatform.getAvailablePlatforms(requireContext()) }
        val assetController = rememberBackgroundAssetControler(shareColors)
        val listener = remember { shareListenerFactory.create(this@ShareEpisodeFragment, assetController, args.source) }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDialogTint(color = shareColors.background.toArgb())
    }

    @Parcelize
    private class Args(
        val podcastUuid: String,
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
                    podcastUuid = episode.podcastUuid,
                    episodeUuid = episode.uuid,
                    baseColor = Color(baseColor),
                    source = source,
                ),
            )
        }
    }
}
