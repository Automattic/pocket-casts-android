package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class SmartPlaylistFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<SmartPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SmartPlaylistViewModel.Factory> { factory ->
                factory.create(playlistUuuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AppThemeWithBackground(theme.activeTheme) {
            SmartPlaylistPage(
                uiState = uiState,
            )
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = SmartPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
