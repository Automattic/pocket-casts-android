
package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.navigation.composable
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NotificationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ObtainConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.rememberColumnState
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object EpisodeScreenFlow {
    const val episodeUuidArgument = "episodeUuid"
    const val route = "episode/{$episodeUuidArgument}"
    fun navigateRoute(episodeUuid: String) = "episode/$episodeUuid"

    // Routes
    const val episodeScreen = "episodeScreen"
    private const val upNextOptionsScreen = "upNextOptionsScreen"
    private const val deleteDownloadConfirmationScreen = "deleteDownloadConfirmationScreen"
    private const val deleteDownloadNotificationScreen = "deleteDownloadNotificationScreen"
    private const val removeFromUpNextNotificationScreen = "removeFromUpNextNotificationScreen"

    fun NavGraphBuilder.episodeGraph(
        navigateToPodcast: (podcastUuid: String) -> Unit,
        navController: NavController,
        swipeToDismissState: SwipeToDismissBoxState,
    ) {
        navigation(
            route = this@EpisodeScreenFlow.route,
            startDestination = episodeScreen,
            arguments = listOf(
                navArgument(episodeUuidArgument) {
                    type = NavType.StringType
                },
            ),
        ) {
            composable(
                route = episodeScreen,
            ) {
                // Listen for results from streaming confirmation screen
                navController.currentBackStackEntry?.savedStateHandle
                    ?.getStateFlow<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.resultKey, null)
                    ?.collectAsStateWithLifecycle()?.value?.let { streamingConfirmationResult ->
                        val viewModel = hiltViewModel<EpisodeViewModel>()
                        LaunchedEffect(streamingConfirmationResult) {
                            viewModel.onStreamingConfirmationResult(streamingConfirmationResult)
                            // Clear result once consumed
                            navController.currentBackStackEntry?.savedStateHandle
                                ?.remove<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.resultKey)
                        }
                    }

                val coroutineScope = rememberCoroutineScope()

                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    EpisodeScreen(
                        columnState = rememberColumnState(
                            factory = ScalingLazyColumnDefaults.belowTimeText(
                                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
                            ),
                        ),
                        navigateToPodcast = navigateToPodcast,
                        navigateToUpNextOptions = { navController.navigate(upNextOptionsScreen) },
                        navigateToConfirmDeleteDownload = {
                            navController.navigate(deleteDownloadConfirmationScreen)
                        },
                        navigateToRemoveFromUpNextNotification = {
                            navController.navigate(removeFromUpNextNotificationScreen)
                        },
                        navigateToStreamingConfirmation = {
                            navController.navigate(StreamingConfirmationScreen.route)
                        },
                        navigateToNowPlaying = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(NowPlayingScreen.pagerIndex)
                            }
                        },
                    )
                }
            }

            composable(
                route = upNextOptionsScreen,
            ) {
                val episodeScreenBackStackEntry = remember(it) {
                    navController.getBackStackEntry(episodeScreen)
                }
                UpNextOptionsScreen(
                    episodeScreenViewModelStoreOwner = episodeScreenBackStackEntry, // Reuse view model from EpisodeScreen
                    onComplete = { navController.popBackStack() },
                )
            }

            composable(
                route = deleteDownloadConfirmationScreen,
            ) {
                // Reuse view model from EpisodeScreen
                val episodeScreenViewModelStoreOwner = remember(it) {
                    navController.getBackStackEntry(episodeScreen)
                }
                val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)

                ObtainConfirmationScreen(
                    text = stringResource(LR.string.podcast_remove_downloaded_file),
                    onConfirm = {
                        viewModel.deleteDownloadedEpisode()
                        navController.navigate(deleteDownloadNotificationScreen) {
                            popUpTo(episodeScreen) {
                                inclusive = false
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }

            composable(
                route = deleteDownloadNotificationScreen,
            ) {
                NotificationScreen(
                    text = stringResource(LR.string.removed),
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = removeFromUpNextNotificationScreen,
            ) {
                NotificationScreen(
                    text = stringResource(LR.string.episode_removed_from_up_next),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
