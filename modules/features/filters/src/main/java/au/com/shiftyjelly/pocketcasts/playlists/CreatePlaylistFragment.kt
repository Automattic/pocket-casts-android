package au.com.shiftyjelly.pocketcasts.playlists

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.playlists.smart.ManageSmartRulesListener
import au.com.shiftyjelly.pocketcasts.playlists.smart.ManageSmartRulesPage
import au.com.shiftyjelly.pocketcasts.playlists.smart.ManageSmartRulesRoutes
import au.com.shiftyjelly.pocketcasts.playlists.smart.RuleType
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
internal class CreatePlaylistFragment : BaseDialogFragment() {
    private var isPlaylistCreated = false

    private val viewModel by viewModels<CreatePlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<CreatePlaylistViewModel.Factory> { factory ->
                factory.create(initialPlaylistName = getString(LR.string.new_playlist))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState by viewModel.uiState.collectAsState()

        OpenCreatedPlaylistEffect()

        DialogBox(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            val navController = rememberNavController()
            val listener = rememberNavigationListener()

            ClearTransientRulesStateEffect(navController)

            ManageSmartRulesPage(
                playlistName = viewModel.playlistNameState.text.toString(),
                appliedRules = uiState.appliedRules,
                rulesBuilder = uiState.rulesBuilder,
                smartEpisodes = uiState.smartEpisodes,
                smartStarredEpisodes = uiState.smartStarredEpisodes,
                followedPodcasts = uiState.followedPodcasts,
                totalEpisodeCount = uiState.totalEpisodeCount,
                useEpisodeArtwork = uiState.useEpisodeArtwork,
                navController = navController,
                listener = listener,
                startDestination = NavigationRoutes.NEW_PLAYLIST,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(NavigationRoutes.NEW_PLAYLIST) {
                    CallOnce {
                        viewModel.trackCreatePlaylistShown()
                    }
                    CreatePlaylistPage(
                        titleState = viewModel.playlistNameState,
                        onCreateManualPlaylist = {
                            viewModel.trackCreateManualPlaylist()
                            viewModel.createManualPlaylist()
                        },
                        onContinueToSmartPlaylist = {
                            viewModel.trackCreateSmartPlaylist()
                            navController.navigateOnce(ManageSmartRulesRoutes.SMART_PLAYLIST_PREVIEW) {
                                popUpTo(NavigationRoutes.NEW_PLAYLIST) {
                                    inclusive = true
                                }
                            }
                        },
                        onClickClose = ::dismiss,
                    )
                }
            }
        }
    }

    @Composable
    private fun rememberNavigationListener() = remember {
        object : ManageSmartRulesListener {
            override fun onChangeUseAllPodcasts(shouldUse: Boolean) = viewModel.useAllPodcasts(shouldUse)

            override fun onSelectPodcast(uuid: String) = viewModel.selectPodcast(uuid)

            override fun onDeselectPodcast(uuid: String) = viewModel.deselectPodcast(uuid)

            override fun onSelectAllPodcasts() = viewModel.selectAllPodcasts()

            override fun onDeselectAllPodcasts() = viewModel.deselectAllPodcasts()

            override fun onChangeUnplayedStatus(shouldUse: Boolean) = viewModel.useUnplayedEpisodes(shouldUse)

            override fun onChangeInProgressStatus(shouldUse: Boolean) = viewModel.useInProgressEpisodes(shouldUse)

            override fun onChangeCompletedStatus(shouldUse: Boolean) = viewModel.useCompletedEpisodes(shouldUse)

            override fun onUseReleaseDate(releaseDate: ReleaseDateRule) = viewModel.useReleaseDate(releaseDate)

            override fun onUseConstrainedDuration(shouldUse: Boolean) = viewModel.useConstrainedDuration(shouldUse)

            override fun onDecrementMinDuration() = viewModel.decrementMinDuration()

            override fun onIncrementMinDuration() = viewModel.incrementMinDuration()

            override fun onDecrementMaxDuration() = viewModel.decrementMaxDuration()

            override fun onIncrementMaxDuration() = viewModel.incrementMaxDuration()

            override fun onUseDownloadStatus(downloadStatus: DownloadStatusRule) = viewModel.useDownloadStatus(downloadStatus)

            override fun onUseMediaType(mediaType: MediaTypeRule) = viewModel.useMediaType(mediaType)

            override fun onUseStarredEpisodes(shouldUse: Boolean) = viewModel.useStarredEpisodes(shouldUse)

            override fun onApplyRule(rule: RuleType) = viewModel.applyRule(rule)

            override fun createPlaylistCallback() = { viewModel.createSmartPlaylist() }

            override fun onClose() = dismiss()
        }
    }

    @Composable
    private fun OpenCreatedPlaylistEffect() {
        LaunchedEffect(Unit) {
            val createdPlaylist = viewModel.createdPlaylist.await()
            isPlaylistCreated = true
            dismiss()
            val fragment = PlaylistFragment.newInstance(createdPlaylist.uuid, createdPlaylist.type)
            (requireActivity() as FragmentHostListener).addFragment(fragment)
        }
    }

    @Composable
    private fun ClearTransientRulesStateEffect(navController: NavController) {
        var currentRoute by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                val newRoute = entry.destination.route
                if (currentRoute != newRoute) {
                    currentRoute = newRoute
                    viewModel.clearTransientRules()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requireActivity().isChangingConfigurations && !isPlaylistCreated) {
            viewModel.trackCreatePlaylistCancelled()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPreFlingThreshold(thresholdDp = 150)
    }
}

private object NavigationRoutes {
    const val NEW_PLAYLIST = "new_playlist"
}
