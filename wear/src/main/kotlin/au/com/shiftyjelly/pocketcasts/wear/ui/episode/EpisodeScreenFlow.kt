package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object EpisodeScreenFlow {
    const val episodeUuidArgument = "episodeUuid"
    const val route = "episode/{$episodeUuidArgument}"
    fun navigateRoute(episodeUuid: String) = "episode/$episodeUuid"

    // Routes
    private const val episodeScreen = "episodeScreen"
    private const val upNextOptionsScreen = "upNextOptionsScreen"
    private const val deleteDownloadConfirmationScreen = "deleteDownloadConfirmationScreen"
    private const val deleteDownloadNotificationScreen = "deleteDownloadNotificationScreen"
    private const val removeFromUpNextNotificationScreen = "removeFromUpNextNotificationScreen"

    fun NavGraphBuilder.episodeGraph(
        navigateToPodcast: (podcastUuid: String) -> Unit,
        navController: NavController,
    ) {
        navigation(
            route = this@EpisodeScreenFlow.route,
            startDestination = episodeScreen,
            arguments = listOf(
                navArgument(episodeUuidArgument) {
                    type = NavType.StringType
                }
            ),
        ) {
            scrollable(
                route = episodeScreen,
                columnStateFactory = ScalingLazyColumnDefaults.belowTimeText(
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top)
                )
            ) {
                EpisodeScreen(
                    columnState = it.columnState,
                    navigateToPodcast = navigateToPodcast,
                    navigateToUpNextOptions = { navController.navigate(upNextOptionsScreen) },
                    navigateToConfirmDeleteDownload = { navController.navigate(deleteDownloadConfirmationScreen) },
                    navigateToRemoveFromUpNextNotification = { navController.navigate(removeFromUpNextNotificationScreen) },
                )
            }

            composable(upNextOptionsScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
                UpNextOptionsScreen(
                    episodeScreenViewModelStoreOwner = navController.getBackStackEntry(episodeScreen), // Reuse view model from EpisodeScreen
                    onComplete = { navController.popBackStack() },
                )
            }

            composable(deleteDownloadConfirmationScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

                // Reuse view model from EpisodeScreen
                val episodeScreenViewModelStoreOwner = navController.getBackStackEntry(episodeScreen)
                val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)

                ObtainConfirmationScreen(
                    text = stringResource(LR.string.podcast_remove_downloaded_file),
                    onConfirm = {
                        viewModel.deleteDownloadedEpisode()
                        navController.navigate(deleteDownloadNotificationScreen) {
                            popUpTo(episodeScreen) {
                                inclusive = true
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }

            composable(deleteDownloadNotificationScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
                NotificationScreen(
                    text = stringResource(LR.string.removed),
                    onClick = { navController.popBackStack() },
                )
            }

            composable(removeFromUpNextNotificationScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
                NotificationScreen(
                    text = stringResource(LR.string.episode_removed_from_up_next),
                    onClick = { navController.popBackStack() },
                )
            }
        }
    }
}
