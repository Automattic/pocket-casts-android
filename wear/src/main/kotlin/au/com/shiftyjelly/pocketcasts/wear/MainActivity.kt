@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.edgeSwipeToDismiss
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.FilesScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.FiltersScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.SettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.UpNextScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authenticationGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.authenticationSubGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable
import com.google.android.horologist.compose.pager.PagerScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme(theme.activeTheme) {
                WearApp()
            }
        }
    }
}

@Composable
private fun WearApp() {

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

            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
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

/**
 * Pager with three pages:
 *
 * 1. [firstPageContent] (passed in as a composable argument)
 * 2. [NowPlayingScreen]
 * 3. [UpNextScreen]
 *
 * Pass a null [swipeToDismissState] to disable swipe to dismiss when the backstack would
 * be empty (i.e., on the first screen in the app).
 */
@Composable
fun NowPlayingPager(
    navController: NavController,
    pagerState: PagerState = rememberPagerState(),
    swipeToDismissState: SwipeToDismissBoxState? = null,
    firstPageContent: @Composable () -> Unit,
) {
    PagerScreen(
        count = 3,
        state = pagerState,
        modifier = if (swipeToDismissState != null) {
            Modifier.edgeSwipeToDismiss(swipeToDismissState)
        } else {
            Modifier
        }
    ) { page ->
        when (page) {

            0 -> firstPageContent()

            1 -> Column {

                // FIXME move this to inside Now Playing Screen???

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

                val coroutineScope = rememberCoroutineScope()

                NowPlayingScreen(
                    navigateToEpisode = { episodeUuid ->
                        coroutineScope.launch {
                            navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                        }
                    },
                    showStreamingConfirmation = { navController.navigate(StreamingConfirmationScreen.route) },
                )
            }

            2 -> Column {
                val scrollableState = rememberScalingLazyListState()
                UpNextScreen(
                    navigateToEpisode = { episodeUuid ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                    },
                    listState = scrollableState,
                )
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        WearApp()
    }
}
