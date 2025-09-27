package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
internal class AddEpisodesFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<AddEpisodesViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AddEpisodesViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
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

        val snackbarHostState = remember { SnackbarHostState() }
        DispatchMessageEffect(snackbarHostState)

        val navController = rememberNavController()
        val searchState = rememberSearchState(navController)
        ClearSearchStateEffect(navController)

        DialogBox(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            AnimatedNonNullVisibility(
                item = viewModel.uiState.collectAsState().value,
                enter = fadeIn,
                exit = fadeOut,
            ) { uiState ->
                AddEpisodesPage(
                    navController = navController,
                    searchState = searchState,
                    playlistTitle = uiState.playlist.title,
                    addedEpisodesCount = uiState.addedEpisodeUuids.size,
                    episodeSources = uiState.sources,
                    folderPodcastsFlow = viewModel::getFolderSourcesFlow,
                    episodesFlow = viewModel::getPodcastEpisodesFlow,
                    hasAnyFolders = uiState.hasAnyFolders,
                    useEpisodeArtwork = uiState.useEpisodeArtwork,
                    onOpenPodcast = {
                        viewModel.trackPodcastTapped()
                    },
                    onOpenFolder = {
                        viewModel.trackFolderTapped()
                    },
                    onAddEpisode = { episodeUuid ->
                        viewModel.trackEpisodeTapped()
                        viewModel.addEpisode(episodeUuid)
                    },
                    onClickNavigationButton = {
                        if (!navController.popBackStack()) {
                            dismiss()
                        }
                    },
                    onClickDoneButton = ::dismiss,
                    modifier = Modifier.Companion.fillMaxSize(),
                )
            }

            ThemedSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    @Composable
    private fun rememberSearchState(navController: NavController): TextFieldState {
        val backStackEntry by navController.currentBackStackEntryAsState()
        return remember(backStackEntry) {
            when (backStackEntry?.destination?.route) {
                AddEpisodesRoutes.PODCAST -> viewModel.podcastSearchState.textState
                AddEpisodesRoutes.FOLDER -> viewModel.folderSearchState.textState
                else -> viewModel.homeSearchState.textState
            }
        }
    }

    @Composable
    private fun DispatchMessageEffect(snackbarState: SnackbarHostState) {
        LaunchedEffect(snackbarState) {
            viewModel.messageQueue.collect { message ->
                val text = when (message) {
                    AddEpisodesViewModel.Message.FailedToAddEpisode -> getString(R.string.add_to_playlist_failure_message)
                }
                snackbarState.showSnackbar(text)
            }
        }
    }

    @Composable
    private fun ClearSearchStateEffect(navController: NavHostController) {
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                if (entry.destination.route != AddEpisodesRoutes.PODCAST) {
                    viewModel.podcastSearchState.textState.clearText()

                    if (entry.destination.route != AddEpisodesRoutes.FOLDER) {
                        viewModel.folderSearchState.textState.clearText()
                    }
                }
            }
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "AddEpisodesFragmentArgs"

        fun newInstance(playlistUuid: String) = AddEpisodesFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()
