@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.edgeSwipeToDismiss
import au.com.shiftyjelly.pocketcasts.wear.ui.UpNextScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import com.google.android.horologist.compose.pager.PagerScreen
import kotlinx.coroutines.launch

/**
 * Pager with three pages:
 *
 * 1. [firstPageContent] (passed in as a composable argument)
 * 2. [NowPlayingScreen]
 * 3. [UpNextScreen]
 */
@Composable
fun NowPlayingPager(
    navController: NavController,
    pagerState: PagerState = rememberPagerState(),
    swipeToDismissState: SwipeToDismissBoxState,
    firstPageContent: @Composable () -> Unit,
) {

    // Don't allow swipe to dismiss on first screen (because there is no where to swipe back to--instead
    // just let the app close) or when the pager is not on the initial page (because we want to avoid
    // swipe to dismiss from the, for example, Now Playing screen, taking the user back to another
    // Now Playing screen.
    val isOnFirstScreen = navController.currentDestination?.route == WatchListScreen.route
    val allowSwipeToDismiss = !isOnFirstScreen && pagerState.currentPage == 0
    val modifier = if (allowSwipeToDismiss) {
        Modifier.edgeSwipeToDismiss(swipeToDismissState)
    } else {
        Modifier
    }

    PagerScreen(
        count = 3,
        state = pagerState,
        modifier = modifier
    ) { page ->
        when (page) {

            0 -> firstPageContent()

            1 -> Column {

                // FIXME move this to inside Now Playing Screen???

                // Listen for results from streaming confirmation screen
                navController.currentBackStackEntry?.savedStateHandle
                    ?.getStateFlow<StreamingConfirmationScreen.Result?>(
                        StreamingConfirmationScreen.resultKey,
                        null
                    )
                    ?.collectAsStateWithLifecycle()?.value?.let { streamingConfirmationResult ->

                        val viewModel = hiltViewModel<NowPlayingViewModel>()
                        LaunchedEffect(streamingConfirmationResult) {
                            viewModel.onStreamingConfirmationResult(streamingConfirmationResult)
                            // Clear result once consumed
                            navController.currentBackStackEntry?.savedStateHandle
                                ?.remove<StreamingConfirmationScreen.Result?>(
                                    StreamingConfirmationScreen.resultKey
                                )
                        }
                    }

                val coroutineScope = rememberCoroutineScope()

                NowPlayingScreen(
                    navigateToEpisode = { episodeUuid ->
                        coroutineScope.launch {

                            val alreadyOnEpisodeScreen =
                                navController.currentDestination?.route == EpisodeScreenFlow.episodeScreen
                            val alreadyOnCorrectEpisode by lazy {
                                navController
                                    .currentBackStackEntry
                                    ?.arguments
                                    ?.getString(EpisodeScreenFlow.episodeUuidArgument)
                                    ?.let { currentScreenEpisodeUuid ->
                                        episodeUuid == currentScreenEpisodeUuid
                                    } ?: false
                            }
                            if (alreadyOnEpisodeScreen && alreadyOnCorrectEpisode) {
                                // Already on the correct episode screen, so just switch back to that page
                                pagerState.animateScrollToPage(0)
                            } else {
                                navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                            }
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
