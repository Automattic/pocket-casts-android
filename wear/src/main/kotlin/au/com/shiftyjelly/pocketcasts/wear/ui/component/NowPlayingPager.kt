package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import au.com.shiftyjelly.pocketcasts.wear.ui.UpNextScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.pager.PagerScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object NowPlayingPager {
    const val PAGE_COUNT = 3
}

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
    swipeToDismissState: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    showTimeText: Boolean = true,
    allowSwipeToDismiss: Boolean = true,
    firstPageContent: @Composable NowPlayingPagerScope.() -> Unit,
) {
    val pagerState = rememberPagerState { NowPlayingPager.PAGE_COUNT }
    val columState = rememberColumnState()
    val pagerScope = remember(pagerState, columState) { NowPlayingPagerScope(pagerState, columState) }

    ScreenScaffold(
        scrollState = columState,
        timeText = if (showTimeText) {
            null
        } else {
            {}
        },
        modifier = modifier,
    ) {
        // Don't allow swipe to dismiss on first screen (because there is no where to swipe back to--instead
        // just let the app close) or when the pager is not on the initial page (because we want to avoid
        // swipe to dismiss from the, for example, Now Playing screen, taking the user back to another
        // Now Playing screen.
        val isSwipeToDismissEnabled by remember(allowSwipeToDismiss, pagerState) {
            snapshotFlow { pagerState.currentPage }.map { allowSwipeToDismiss && it == 0 }
        }.collectAsState(initial = false)

        PagerScreen(
            state = pagerState,
            modifier = if (isSwipeToDismissEnabled) {
                Modifier.edgeSwipeToDismiss(swipeToDismissState)
            } else {
                Modifier
            },
        ) { page ->
            when (page) {
                0 -> firstPageContent(pagerScope)

                1 -> Column {
                    val coroutineScope = rememberCoroutineScope()

                    NowPlayingScreen(
                        navigateToEpisode = { episodeUuid ->
                            coroutineScope.launch {
                                val alreadyOnEpisodeScreen =
                                    navController.currentDestination?.route == EpisodeScreenFlow.EPISODE_SCREEN
                                val alreadyOnCorrectEpisode by lazy {
                                    navController
                                        .currentBackStackEntry
                                        ?.arguments
                                        ?.getString(EpisodeScreenFlow.EPISODE_UUID_ARGUMENT)
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
                        navController = navController,
                    )
                }

                2 -> Column {
                    UpNextScreen(
                        navigateToEpisode = { episodeUuid ->
                            navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                        },
                        columnState = rememberColumnState(),
                    )
                }
            }
        }
    }
}

class NowPlayingPagerScope(
    val pagerState: PagerState,
    val columnState: ScalingLazyColumnState,
)
