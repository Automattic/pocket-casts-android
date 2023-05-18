@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.FilesScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.FiltersScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.LoggingInScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.SettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationNavGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.player.PCVolumeScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: WearMainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            WearAppTheme(theme.activeTheme) {
                WearApp(
                    signInConfirmationAction = state.signInConfirmationAction,
                    onSignInConfirmationActionHandled = viewModel::onSignInConfirmationActionHandled,
                )
            }
        }
    }
}

@Composable
fun WearApp(
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
) {

    val navController = rememberSwipeDismissableNavController()
    val swipeToDismissState = rememberSwipeToDismissBoxState()
    val navState = rememberSwipeDismissableNavHostState(swipeToDismissState)

    handleSignInConfirmation(
        signInConfirmationAction = signInConfirmationAction,
        onSignInConfirmationActionHandled = onSignInConfirmationActionHandled,
        navController = navController
    )

    WearNavScaffold(
        navController = navController,
        startDestination = WatchListScreen.route,
        state = navState,
    ) {

        scrollable(
            route = WatchListScreen.route,
        ) {
            val pagerState = rememberPagerState()
            val coroutineScope = rememberCoroutineScope()
            NowPlayingPager(
                navController = navController,
                pagerState = pagerState,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {

                navController.currentBackStackEntry?.savedStateHandle
                    ?.getStateFlow(WatchListScreen.scrollToTop, false)
                    ?.collectAsStateWithLifecycle()?.value?.let { scrollToTop ->
                        if (scrollToTop) {
                            coroutineScope.launch {
                                it.scrollableState.scrollToItem(0)
                            }
                            // Reset once consumed
                            navController.currentBackStackEntry?.savedStateHandle
                                ?.set(WatchListScreen.scrollToTop, false)
                        }
                    }

                WatchListScreen(
                    scrollState = it.scrollableState,
                    navigateToRoute = navController::navigate,
                    toNowPlaying = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                )
            }
        }

        composable(
            route = PCVolumeScreen.route,
        ) {
            it.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
            PCVolumeScreen()
        }

        composable(StreamingConfirmationScreen.route) {
            it.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            StreamingConfirmationScreen(
                onFinished = { result ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        StreamingConfirmationScreen.resultKey,
                        result
                    )
                    navController.popBackStack()
                },
            )
        }

        scrollable(
            route = PodcastsScreen.route,
        ) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {
                PodcastsScreen(
                    listState = it.scrollableState,
                    navigateToPodcast = { podcastUuid ->
                        navController.navigate(PodcastScreen.navigateRoute(podcastUuid))
                    }
                )
            }
        }

        composable(
            route = PodcastScreen.route,
            arguments = listOf(
                navArgument(PodcastScreen.argument) {
                    type = NavType.StringType
                }
            ),
        ) {

            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
            ) {
                PodcastScreen(
                    onEpisodeTap = { episode ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                    },
                )
            }
        }

        episodeGraph(
            navigateToPodcast = { podcastUuid ->
                navController.navigate(PodcastScreen.navigateRoute(podcastUuid))
            },
            navController = navController,
            swipeToDismissState = swipeToDismissState,
        )

        composable(FiltersScreen.route) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
            ) {
                FiltersScreen()
            }
        }

        scrollable(DownloadsScreen.route) {

            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {
                DownloadsScreen(
                    columnState = it.columnState,
                    onItemClick = { episode ->
                        val route = EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid)
                        navController.navigate(route)
                    }
                )
            }
        }

        scrollable(FilesScreen.route) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
            ) {
                FilesScreen(
                    columnState = it.columnState,
                    navigateToEpisode = { episodeUuid ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                    },
                )
            }
        }

        scrollable(SettingsScreen.route) {
            SettingsScreen(
                scrollState = it.columnState,
                signInClick = { navController.navigate(authenticationSubGraph) },
            )
        }

        authenticationNavGraph(navController)

        composable(LoggingInScreen.routeWithDelay) {
            LoggingInScreen(
                onClose = { navController.popBackStack() },
                // Because this login is not triggered by the user, make sure that the
                // logging in screen is shown for enough time for the user to understand
                // what is happening.
                withMinimumDelay = true,
            )
        }

        composable(LoggingInScreen.route) {
            LoggingInScreen(
                onClose = { WatchListScreen.popToTop(navController) },
            )
        }
    }
}

private fun handleSignInConfirmation(
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
    navController: NavController,
) {

    val signInNotificationShowing = navController.currentDestination?.route == LoggingInScreen.routeWithDelay

    when (signInConfirmationAction) {

        is SignInConfirmationAction.Show -> {
            if (!signInNotificationShowing) {
                navController.navigate(LoggingInScreen.routeWithDelay)
            }
        }

        SignInConfirmationAction.Hide -> {
            if (signInNotificationShowing) {
                navController.popBackStack()
            }
        }

        null -> { /* do nothing */ }
    }

    onSignInConfirmationActionHandled()
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        signInConfirmationAction = null,
        onSignInConfirmationActionHandled = {},
    )
}
