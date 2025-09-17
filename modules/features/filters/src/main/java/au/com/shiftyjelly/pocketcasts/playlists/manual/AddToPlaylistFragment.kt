package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
internal class AddToPlaylistFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<AddToPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AddToPlaylistViewModel.Factory> { factory ->
                factory.create(args.episodeUuid, initialPlaylistTitle = getString(LR.string.new_playlist))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val navController = rememberNavController()

        OpenCreatedPlaylistEffect()

        DialogBox(
            themeType = args.customTheme ?: theme.activeTheme,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AddToPlaylistPage(
                navController = navController,
                newPlaylistNameState = viewModel.newPlaylistNameState,
                onClickDoneButton = ::dismiss,
                onClickCreatePlaylist = viewModel::createPlaylist,
                onClickNavigationButton = {
                    if (!navController.popBackStack()) {
                        dismiss()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun OpenCreatedPlaylistEffect() {
        LaunchedEffect(Unit) {
            val playlistUuid = viewModel.createdPlaylist.await()

            dismiss()
            val hostListener = requireActivity() as FragmentHostListener
            hostListener.closeFiltersToRoot()
            hostListener.addFragment(PlaylistFragment.newInstance(playlistUuid, Playlist.Type.Manual))
            hostListener.closeBottomSheet()
            hostListener.closePlayer()
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val customTheme: Theme.ThemeType?,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "AddToPlaylistFragmentArgs"

        fun newInstance(
            episodeUuid: String,
            customTheme: Theme.ThemeType? = null,
        ) = AddToPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(episodeUuid, customTheme))
        }
    }
}
