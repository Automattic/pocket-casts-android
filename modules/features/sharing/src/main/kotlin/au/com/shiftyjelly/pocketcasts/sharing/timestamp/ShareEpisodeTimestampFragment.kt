package au.com.shiftyjelly.pocketcasts.sharing.timestamp

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
import au.com.shiftyjelly.pocketcasts.utils.parceler.DurationParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@AndroidEntryPoint
class ShareEpisodeTimestampFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val shareColors get() = ShareColors(args.baseColor)

    private val viewModel by viewModels<ShareEpisodeTimestampViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ShareEpisodeTimestampViewModel.Factory> { factory ->
                factory.create(args.episodeUuid)
            }
        },
    )

    @Inject internal lateinit var shareListenerFactory: ShareEpisodeTimestampListener.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        val platforms = SocialPlatform.getAvailablePlatforms(requireContext())
        val assetController = BackgroundAssetController.create(requireContext(), shareColors)
        val listener = shareListenerFactory.create(this@ShareEpisodeTimestampFragment, assetController, args.timestampType, args.source)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            ShareEpisodeTimestampPage(
                podcast = uiState.podcast,
                episode = uiState.episode,
                timestamp = args.timestamp,
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
        @TypeParceler<Duration, DurationParceler>() val timestamp: Duration,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
        val timestampType: TimestampType,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ShareEpisodeFragmentArgs"

        fun forEpisodePosition(
            episode: PodcastEpisode,
            @ColorInt baseColor: Int,
            source: SourceView,
        ) = ShareEpisodeTimestampFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    episodeUuid = episode.uuid,
                    timestamp = episode.playedUpTo.seconds,
                    baseColor = Color(baseColor),
                    source = source,
                    timestampType = TimestampType.Episode,
                ),
            )
        }

        fun forBookmark(
            episode: PodcastEpisode,
            timestamp: Duration,
            @ColorInt baseColor: Int,
            source: SourceView,
        ) = ShareEpisodeTimestampFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(
                    episodeUuid = episode.uuid,
                    timestamp = timestamp,
                    baseColor = Color(baseColor),
                    source = source,
                    timestampType = TimestampType.Bookmark,
                ),
            )
        }
    }
}
