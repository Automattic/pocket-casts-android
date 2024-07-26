package au.com.shiftyjelly.pocketcasts.sharing.timestamp

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.sp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.utils.parceler.DurationParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

class ShareEpisodeTimestampFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireActivity()).apply {
        setContent {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxSize(),
            ) {
                Text(
                    text = "Share ${if (args.shareAsBookmark) "bookmark" else "episode timestamp"}: ${args.episodeUuid}",
                    fontSize = 24.sp,
                    color = Color.White,
                )
            }
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        @TypeParceler<Duration, DurationParceler>() val timestamp: Duration,
        @TypeParceler<Color, ColorParceler>() val baseColor: Color,
        val source: SourceView,
        val shareAsBookmark: Boolean,
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
                    podcastUuid = episode.podcastUuid,
                    timestamp = episode.playedUpTo.seconds,
                    baseColor = Color(baseColor),
                    source = source,
                    shareAsBookmark = false,
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
                    podcastUuid = episode.podcastUuid,
                    timestamp = timestamp,
                    baseColor = Color(baseColor),
                    source = source,
                    shareAsBookmark = true,
                ),
            )
        }
    }
}
