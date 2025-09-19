package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory.Source
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
internal class AddToPlaylistFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<AddToPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AddToPlaylistViewModel.Factory> { factory ->
                factory.create(
                    source = args.source,
                    episodeUuid = args.episodeUuid,
                    initialPlaylistTitle = getString(LR.string.new_playlist),
                )
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            viewModel.trackScreenShown()
        }

        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val navController = rememberNavController()

        OpenCreatedPlaylistEffect()

        DialogBox(
            themeType = args.customTheme ?: theme.activeTheme,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AnimatedNonNullVisibility(
                item = viewModel.uiState.collectAsState().value,
                enter = fadeIn,
                exit = fadeOut,
            ) { uiState ->
                AddToPlaylistPage(
                    playlistPreviews = uiState.playlistPreviews,
                    unfilteredPlaylistsCount = uiState.unfilteredPlaylistsCount,
                    navController = navController,
                    searchFieldState = viewModel.searchFieldState.textState,
                    newPlaylistNameState = viewModel.newPlaylistNameState,
                    onClickCreatePlaylist = {
                        viewModel.trackCreateNewPlaylistTapped()
                        viewModel.createPlaylist()
                    },
                    onChangeEpisodeInPlaylist = { playlist ->
                        if (playlist.canAddOrRemoveEpisode) {
                            if (playlist.hasEpisode) {
                                viewModel.trackEpisodeRemoveTapped()
                                viewModel.removeFromPlaylist(playlist.uuid)
                            } else {
                                viewModel.trackEpisodeAddTapped(isPlaylistFull = false)
                                viewModel.addToPlaylist(playlist.uuid)
                            }
                        } else {
                            viewModel.trackEpisodeAddTapped(isPlaylistFull = true)
                            if (snackbarHostState.currentSnackbarData == null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(getString(LR.string.playlist_is_full_description))
                                }
                            }
                        }
                    },
                    onClickContinueWithNewPlaylist = {
                        viewModel.trackNewPlaylistTapped()
                    },
                    onClickDoneButton = ::dismiss,
                    onClickNavigationButton = {
                        if (!navController.popBackStack()) {
                            dismiss()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ThemedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
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
        val source: Source,
        val episodeUuid: String,
        val customTheme: Theme.ThemeType?,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "AddToPlaylistFragmentArgs"

        fun newInstance(
            source: Source,
            episodeUuid: String,
            customTheme: Theme.ThemeType? = null,
        ) = AddToPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(source, episodeUuid, customTheme))
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
