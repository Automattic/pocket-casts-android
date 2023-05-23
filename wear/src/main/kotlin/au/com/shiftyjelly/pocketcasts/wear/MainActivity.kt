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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.FilesScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.FiltersScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.LoggingInScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.SettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.RequirePlusScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationNavGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.player.EffectsScreen
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
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: WearMainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme(theme.activeTheme) {

                val state by viewModel.state.collectAsState()

                WearApp(
                    signInState = state.signInState,
                    subscriptionStatus = state.subscriptionStatus,
                    showLoggingInScreen = state.showLoggingInScreen,
                    onLoggingInScreenShown = viewModel::onSignInConfirmationActionHandled,
                    signOut = viewModel::signOut,
                )
            }
        }
    }
}

@Composable
fun WearApp(
    signInState: SignInState?,
    subscriptionStatus: SubscriptionStatus?,
    showLoggingInScreen: Boolean,
    onLoggingInScreenShown: () -> Unit,
    signOut: () -> Unit,
) {

    val navController = rememberSwipeDismissableNavController()
    val swipeToDismissState = rememberSwipeToDismissBoxState()
    val navState = rememberSwipeDismissableNavHostState(swipeToDismissState)
    var loaded by remember { mutableStateOf(false) }

    if (showLoggingInScreen) {
        navController.navigate(LoggingInScreen.routeWithDelay)
        onLoggingInScreenShown()
    }

    val userCanAccessWatch = when (subscriptionStatus) {
        is SubscriptionStatus.Free,
        SubscriptionStatus.NotSignedIn,
        null -> false
        is SubscriptionStatus.Plus -> true
    }

    var waitingForSignIn by remember { mutableStateOf(false) }
    if (!userCanAccessWatch) {
        waitingForSignIn = true
    }

    val startDestination = if (userCanAccessWatch) WatchListScreen.route else RequirePlusScreen.route

    WearNavScaffold(
        navController = navController,
        startDestination = startDestination,
        state = navState,
    ) {

        scrollable(RequirePlusScreen.route) {
            loaded = true
            RequirePlusScreen(
                columnState = it.columnState,
                onContinueToLogin = { navController.navigate(authenticationSubGraph) },
            )
        }

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

        val popToStartDestination: () -> Unit = {
            when (startDestination) {
                WatchListScreen.route -> {
                    val popped = navController.popBackStack(
                        route = WatchListScreen.route,
                        inclusive = false,
                    )
                    if (popped) {
                        navController
                            .currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(WatchListScreen.scrollToTop, true)
                    }
                }

                RequirePlusScreen.route -> {
                    navController.popBackStack(
                        route = RequirePlusScreen.route,
                        inclusive = false,
                    )
                }

                else -> throw IllegalStateException("Unexpected start destination $startDestination")
            }
        }

        authenticationNavGraph(
            navController = navController,
            onEmailSignInSuccess = {
                navController.navigate(LoggingInScreen.route)
            },
            googleSignInSuccessScreen = { googleSignInAccount ->
                LoggingInScreen(
                    avatarUrl = googleSignInAccount?.photoUrl?.toString(),
                    name = googleSignInAccount?.givenName,
                    onClose = {}
                )
            }
        )

        loggingInScreens(onClose = { popToStartDestination() })

        composable(PCVolumeScreen.route) {
            it.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            PCVolumeScreen()
        }

        scrollable(EffectsScreen.route,) {
            EffectsScreen(
                columnState = it.columnState,
            )
        }
    }

    // We cannot use the subscription status contained in the SignInState object because the subscription status
    // gets updated to the correct value a bit after the user signs in—there is a delay in getting the updated and
    // correct subscription status. For example, immediately after a user signs in with a plus account, their
    // sign in state does not report that it is a Plus subscription until after the subscription call completes.
    // This has to happen after the WearNavScaffold so that the new start destination has been processed,
    // otherwise the new start destination will replace any navigation we do here to the LoggingInScreen.
    var previousSubscriptionStatus by remember { mutableStateOf<SubscriptionStatus?>(null) }
    if (previousSubscriptionStatus != subscriptionStatus &&
        signInState is SignInState.SignedIn
    ) {

        when (subscriptionStatus) {
            is SubscriptionStatus.Free,
            SubscriptionStatus.NotSignedIn,
            null -> {
                // This gets the user back to the start destination if they logged in as free
                signOut()
                navController.popBackStack(startDestination, inclusive = false)
            }
            is SubscriptionStatus.Plus -> {
                if (waitingForSignIn) {
                    navController.navigate(LoggingInScreen.route)
                }
            }
        }
    }
    previousSubscriptionStatus = subscriptionStatus
}

private fun NavGraphBuilder.loggingInScreens(
    onClose: () -> Unit,
) {
    composable(LoggingInScreen.route) {
        Timber.i("navigating to logging in screen")
        LoggingInScreen(onClose = onClose)
    }

    composable(LoggingInScreen.routeWithDelay) {
        LoggingInScreen(
            onClose = onClose,
            // Because this login is not triggered by the user, make sure that the
            // logging in screen is shown for enough time for the user to understand
            // what is happening.
            withMinimumDelay = true,
        )
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        signInState = null,
        subscriptionStatus = null,
        showLoggingInScreen = false,
        onLoggingInScreenShown = {},
        signOut = {},
    )
}
