package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class PlaylistFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                TextP40("Manual playlist: ${args.playlistUuid}", modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "ManualPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
