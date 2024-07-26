package au.com.shiftyjelly.pocketcasts.sharing.podcast

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.utils.parceler.ColorParceler
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

class SharePodcastFragment : BaseDialogFragment() {
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
                    text = "Share podcast: ${args.podcastUuid}",
                    fontSize = 24.sp,
                    color = Color.White,
                )
            }
        }
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
