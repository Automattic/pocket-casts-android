@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
<<<<<<< HEAD
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
=======
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
>>>>>>> origin/main
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
import au.com.shiftyjelly.pocketcasts.wear.ui.authenticationGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.authenticationSubGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
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
<<<<<<< HEAD
            WearAppTheme(theme.activeTheme) {
                WearApp()
            }
=======
            val state by viewModel.state.collectAsState()
            WearApp(
                themeType = theme.activeTheme,
                signInConfirmationAction = state.signInConfirmationAction,
                onSignInConfirmationActionHandled = viewModel::onSignInConfirmationActionHandled,
            )
>>>>>>> origin/main
        }
    }
}

@Composable
<<<<<<< HEAD
private fun WearApp() {
=======
fun WearApp(
    themeType: Theme.ThemeType,
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
) {
    WearAppTheme(themeType) {

        val navController = rememberSwipeDismissableNavController()
>>>>>>> origin/main

    val navController = rememberSwipeDismissableNavController()
    val swipeToDismissState = rememberSwipeToDismissBoxState()
    val navState = rememberSwipeDismissableNavHostState(swipeToDismissState)

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
            ) {
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

<<<<<<< HEAD
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
=======
            handleSignInConfirmation(
                signInConfirmationAction = signInConfirmationAction,
                onSignInConfirmationActionHandled = onSignInConfirmationActionHandled,
                navController = navController
            )

            scrollable(
                route = WatchListScreen.route,
                columnStateFactory = ScalingLazyColumnDefaults.belowTimeText()
            ) {
                WatchListScreen(navController::navigate, it.scrollableState)
            }

            composable(NowPlayingScreen.route) {
                it.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

                // Listen for results from streaming confirmation screen
                navController.currentBackStackEntry?.savedStateHandle
                    ?.getStateFlow<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.resultKey, null)
                    ?.collectAsStateWithLifecycle()?.value?.let { streamingConfirmationResult ->
                        val viewModel = hiltViewModel<NowPlayingViewModel>()
                        LaunchedEffect(streamingConfirmationResult) {
                            viewModel.onStreamingConfirmationResult(streamingConfirmationResult)
                            // Clear result once consumed
                            navController.currentBackStackEntry?.savedStateHandle
                                ?.remove<StreamingConfirmationScreen.Result?>(StreamingConfirmationScreen.resultKey)
                        }
                    }

                NowPlayingScreen(
                    navigateToEpisode = { episodeUuid ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                    },
                    showStreamingConfirmation = { navController.navigate(StreamingConfirmationScreen.route) },
                )
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
                route = UpNextScreen.route,
            ) {
                UpNextScreen(
                    navigateToEpisode = { episodeUuid ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                    },
                    listState = it.scrollableState,
                )
            }

            scrollable(
                route = PodcastsScreen.route,
>>>>>>> origin/main
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
            ) {
                DownloadsScreen(
                    columnState = it.columnState,
                    onItemClick = { episode ->
                        val route = EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid)
                        navController.navigate(route)
                    }
                )
            }
<<<<<<< HEAD
=======

            composable(FilesScreen.route) { FilesScreen() }

            scrollable(SettingsScreen.route) {
                SettingsScreen(
                    scrollState = it.columnState,
                    signInClick = { navController.navigate(authenticationSubGraph) },
                )
            }

            authenticationGraph(navController)

            composable(LoggingInScreen.route) {
                LoggingInScreen(
                    onClose = { navController.popBackStack() },
                )
            }
>>>>>>> origin/main
        }

        composable(FilesScreen.route) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
            ) {
                FilesScreen()
            }
        }

        scrollable(SettingsScreen.route) {
            SettingsScreen(
                scrollState = it.columnState,
                signInClick = { navController.navigate(authenticationSubGraph) },
            )
        }

        authenticationGraph(navController)
    }
}

private fun handleSignInConfirmation(
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
    navController: NavController,
) {

    val signInNotificationShowing = navController.currentDestination?.route == LoggingInScreen.route

    when (signInConfirmationAction) {

        is SignInConfirmationAction.Show -> {
            if (!signInNotificationShowing) {
                navController.navigate(LoggingInScreen.route)
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
<<<<<<< HEAD
    WearAppTheme(Theme.ThemeType.DARK) {
        WearApp()
    }
=======
    WearApp(
        themeType = Theme.ThemeType.DARK,
        signInConfirmationAction = null,
        onSignInConfirmationActionHandled = {},
    )
>>>>>>> origin/main
}
