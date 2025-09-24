package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoDownloadSettingsViewModel
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutoDownloadSettingsFragment :
    BaseFragment(),
    HasBackstack {

    private val viewModel by viewModels<AutoDownloadSettingsViewModel>()

    @Inject
    lateinit var settings: Settings

    private var navController: NavHostController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val uiState by viewModel.uiState.collectAsState()
            val miniPlayerInset by settings.bottomInset.collectAsState(0)

            val snackbarHostState = remember { SnackbarHostState() }

            val navController = rememberNavController()
            this.navController = navController

            LaunchedEffect(navController) {
                navController.currentBackStackEntryFlow.collect { entry ->
                    val route = entry.destination.route?.let(AutoDownloadSettingsRoute::fromValue)
                    if (route != null) {
                        viewModel.trackPageShown(route)
                    }
                }
            }

            Box(
                modifier = Modifier.padding(
                    bottom = miniPlayerInset.pxToDp(requireContext()).dp,
                ),
            ) {
                AutoDownloadSettingsPage(
                    uiState = uiState,
                    navController = navController,
                    onChangeUpNextDownload = viewModel::changeUpNextDownload,
                    onChangeNewEpisodesDownload = viewModel::changeNewEpisodesDownload,
                    onChangeOnFollowDownload = viewModel::changeOnFollowDownload,
                    onChangeAutoDownloadLimitSetting = ::showEpisodeLimitDialog,
                    onChangeOnUnmeteredDownload = viewModel::changeOnUnmeteredDownload,
                    onChangeOnlyWhenChargingDownload = viewModel::changeOnlyWhenChargingDownload,
                    onChangePodcast = viewModel::changePodcastAutoDownload,
                    onChangeAllPodcasts = viewModel::changeAllPodcastsAutoDownload,
                    onChangePlaylist = viewModel::changePlaylistAutoDownload,
                    onChangeAllPlaylists = viewModel::changeAllPlaylistsAutoDownload,
                    onStopAllDownloads = {
                        viewModel.stopAllDownloads()
                        viewLifecycleOwner.lifecycleScope.launch {
                            snackbarHostState.showSnackbar(getString(LR.string.settings_auto_download_stopping_all))
                        }
                    },
                    onClearDownloadErrors = {
                        viewModel.clearDownloadErrors()
                        viewLifecycleOwner.lifecycleScope.launch {
                            snackbarHostState.showSnackbar(getString(LR.string.settings_auto_download_clearing_errors))
                        }
                    },
                    onDismiss = {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    },
                )

                ThemedSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.navController = null
    }

    private fun showEpisodeLimitDialog() {
        if (childFragmentManager.findFragmentByTag("auto-download-episode-limit") != null) {
            return
        }
        AutoDownloadLimitFragment().show(childFragmentManager, "auto-download-episode-limit")
    }

    override fun getBackstackCount(): Int {
        val composeNavigator = navController?.navigatorProvider?.get(ComposeNavigator::class)
        val backStackSize = composeNavigator?.backStack?.value?.size?.minus(1) ?: 0
        return backStackSize + super.getBackstackCount()
    }

    override fun onBackPressed(): Boolean {
        return navController?.popBackStack() == true || super.onBackPressed()
    }
}
