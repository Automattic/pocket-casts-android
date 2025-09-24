
package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    const val EPISODE_UUID_ARGUMENT = "episodeUuid"
    const val ROUTE = "episode/{$EPISODE_UUID_ARGUMENT}"
    fun navigateRoute(episodeUuid: String) = "episode/$episodeUuid"

    // Routes
    const val EPISODE_SCREEN = "episodeScreen"
    private const val UP_NEXT_OPTIONS_SCREEN = "upNextOptionsScreen"
    private const val DELETE_DOWNLOAD_CONFIRMATION_SCREEN = "deleteDownloadConfirmationScreen"
    private const val DELETE_DOWNLOAD_NOTIFICATION_SCREEN = "deleteDownloadNotificationScreen"
    private const val REMOVE_FROM_UP_NEXT_NOTIFICAGTIONS_SCREEN = "removeFromUpNextNotificationScreen"

    fun NavGraphBuilder.episodeGraph(
        navigateToPodcast: (podcastUuid: String) -> Unit,
        navController: NavController,
        swipeToDismissState: SwipeToDismissBoxState,
    ) {
        navigation(
            route = this@EpisodeScreenFlow.ROUTE,
            startDestination = EPISODE_SCREEN,
            arguments = listOf(
                navArgument(EPISODE_UUID_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) {
            composable(
                route = EPISODE_SCREEN,
            ) {
                // Listen for results from streaming confirmation screen
                navController.currentBackStackEntry?.savedStateHandle
                    ?.getStateFlow<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.RESULT_KEY, null)
                    ?.collectAsStateWithLifecycle()?.value?.let { streamingConfirmationResult ->
                        val viewModel = hiltViewModel<EpisodeViewModel>()
                        LaunchedEffect(streamingConfirmationResult) {
                            viewModel.onStreamingConfirmationResult(streamingConfirmationResult)
                            // Clear result once consumed
                            navController.currentBackStackEntry?.savedStateHandle
                                ?.remove<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.RESULT_KEY)
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
                        navigateToUpNextOptions = { navController.navigate(UP_NEXT_OPTIONS_SCREEN) },
                        navigateToConfirmDeleteDownload = {
                            navController.navigate(DELETE_DOWNLOAD_CONFIRMATION_SCREEN)
                        },
                        navigateToRemoveFromUpNextNotification = {
                            navController.navigate(REMOVE_FROM_UP_NEXT_NOTIFICAGTIONS_SCREEN)
                        },
                        navigateToStreamingConfirmation = {
                            navController.navigate(StreamingConfirmationScreen.ROUTE)
                        },
                        navigateToNowPlaying = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(NowPlayingScreen.PAGER_INDEX)
                            }
                        },
                    )
                }
            }

            composable(
                route = UP_NEXT_OPTIONS_SCREEN,
            ) {
                val episodeScreenBackStackEntry = remember(it) {
                    navController.getBackStackEntry(EPISODE_SCREEN)
                }
                UpNextOptionsScreen(
                    episodeScreenViewModelStoreOwner = episodeScreenBackStackEntry, // Reuse view model from EpisodeScreen
                    onComplete = { navController.popBackStack() },
                )
            }

            composable(
                route = DELETE_DOWNLOAD_CONFIRMATION_SCREEN,
            ) {
                // Reuse view model from EpisodeScreen
                val episodeScreenViewModelStoreOwner = remember(it) {
                    navController.getBackStackEntry(EPISODE_SCREEN)
                }
                val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)

                ObtainConfirmationScreen(
                    text = stringResource(LR.string.podcast_remove_downloaded_file),
                    onConfirm = {
                        viewModel.deleteDownloadedEpisode()
                        navController.navigate(DELETE_DOWNLOAD_NOTIFICATION_SCREEN) {
                            popUpTo(EPISODE_SCREEN) {
                                inclusive = false
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }

            composable(
                route = DELETE_DOWNLOAD_NOTIFICATION_SCREEN,
            ) {
                NotificationScreen(
                    text = stringResource(LR.string.removed),
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = REMOVE_FROM_UP_NEXT_NOTIFICAGTIONS_SCREEN,
            ) {
                NotificationScreen(
                    text = stringResource(LR.string.episode_removed_from_up_next),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
