package au.com.shiftyjelly.pocketcasts.sharing.podcast

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class SharePodcastFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val shareColors get() = ShareColors(args.baseColor)

    private val viewModel by viewModels<SharePodcastViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SharePodcastViewModel.Factory> { factory ->
                factory.create(args.podcastUuid)
            }
        },
    )

    @Inject internal lateinit var shareListenerFactory: SharePodcastListener.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val platforms = SocialPlatform.getAvailablePlatforms(requireContext())
        val listener = shareListenerFactory.create(this@SharePodcastFragment, args.source)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            SharePodcastPage(
                podcast = uiState.podcast,
                episodeCount = uiState.episodeCount,
                socialPlatforms = platforms,
                shareColors = shareColors,
                listener = listener,
                rememberCaptureController(),
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        styleBackgroundColor(shareColors.background.toArgb())
    }

    @Parcelize
    private class Args(
        val podcastUuid: String,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "SharePodcastFragmentArgs"

        fun newInstance(
            podcast: Podcast,
            source: SourceView,
        ) = SharePodcastFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    podcastUuid = podcast.uuid,
                    baseColor = Color(podcast.backgroundColor),
                    source = source,
                ),
            )
        }
    }
}
