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
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistPreviewForEpisode
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory.Source
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
internal class AddToPlaylistFragment : BaseDialogFragment() {
    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARGS)

    private val viewModel by viewModels<AddToPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AddToPlaylistViewModel.Factory> { factory ->
                factory.create(
                    source = args.source,
                    episodeUuid = args.episodeUuid,
                    podcastUuid = args.podcastUuid,
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
                    episodeLimit = uiState.episodeLimit,
                    navController = navController,
                    searchFieldState = viewModel.searchFieldState.textState,
                    newPlaylistNameState = viewModel.newPlaylistNameState,
                    getArtworkUuidsFlow = viewModel::getArtworkUuidsFlow,
                    refreshArtworkUuids = viewModel::refreshArtworkUuids,
                    onClickCreatePlaylist = {
                        viewModel.trackCreateNewPlaylistTapped()
                        viewModel.createPlaylist()
                    },
                    onChangeEpisodeInPlaylist = { playlist ->
                        if (playlist.canAddOrRemoveEpisode(uiState.episodeLimit)) {
                            if (playlist.hasEpisode) {
                                viewModel.trackEpisodeRemoveTapped(playlist)
                                viewModel.removeFromPlaylist(playlist.uuid)
                            } else {
                                viewModel.trackEpisodeAddTapped(playlist, isPlaylistFull = false)
                                viewModel.addToPlaylist(playlist.uuid)
                            }
                        } else {
                            viewModel.trackEpisodeAddTapped(playlist, isPlaylistFull = true)
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
                    onClickDoneButton = {
                        showDoneSnackbar(viewModel.getPlaylistsAddedTo())
                        dismiss()
                    },
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

    private fun showDoneSnackbar(playlistsAddedTo: Set<PlaylistPreviewForEpisode>) {
        val hostListener = requireActivity() as FragmentHostListener
        val snackbarView = hostListener.snackBarView()
        val snackbar = when (val size = playlistsAddedTo.size) {
            0 -> return

            1 -> {
                val playlist = playlistsAddedTo.first()
                val message = getString(LR.string.added_to_playlist_single, playlist.title)
                Snackbar.make(snackbarView, message, Snackbar.LENGTH_LONG)
                    .setAction(LR.string.view) { hostListener.openManualPlaylist(playlist.uuid) }
            }

            else -> {
                val message = resources.getQuantityString(LR.plurals.added_to_playlist_single_multiple, size, size)
                Snackbar.make(snackbarView, message, Snackbar.LENGTH_LONG)
            }
        }
        snackbar.show()
    }

    @Composable
    private fun OpenCreatedPlaylistEffect() {
        LaunchedEffect(Unit) {
            val playlistUuid = viewModel.createdPlaylist.await()

            dismiss()
            (requireActivity() as FragmentHostListener).openManualPlaylist(playlistUuid)
        }
    }

    @Parcelize
    private class Args(
        val source: Source,
        val episodeUuid: String,
        val podcastUuid: String,
        val customTheme: Theme.ThemeType?,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "AddToPlaylistFragmentArgs"

        fun newInstance(
            source: Source,
            episodeUuid: String,
            podcastUuid: String,
            customTheme: Theme.ThemeType? = null,
        ) = AddToPlaylistFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARGS to Args(
                    source = source,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                    customTheme = customTheme,
                ),
            )
        }
    }
}

// This is deliberately not a member function to avoid memory leaks when showing a snackbar.
// If this were a member function of the fragment, any lambda (such as the one passed to setAction())
// that references it would capture the fragment's `this` reference, potentially causing a memory leak.
// Making it an extension function on FragmentHostListener ensures only the activity and playlistUuid are captured.
private fun FragmentHostListener.openManualPlaylist(playlistUuid: String) {
    closeFiltersToRoot()
    addFragment(PlaylistFragment.newInstance(playlistUuid, Playlist.Type.Manual))
    closeBottomSheet()
    closePlayer()
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
