@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.FilesScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.LoggingInScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.ScrollToTop
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.RequirePlusScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationNavGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationSubGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.filter.FilterScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.filters.FiltersScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.EffectsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.PCVolumeScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.PrivacySettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.SettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.UrlScreenRoutes
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.WearAboutScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.settingsUrlScreens
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.ScrollableScaffoldContext
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WearMainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearAppTheme {

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

    override fun onResume() {
        super.onResume()
        viewModel.refreshPodcasts()
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

    if (showLoggingInScreen) {
        navController.navigate(LoggingInScreen.routeWithDelay)
        onLoggingInScreenShown()
    }

    val userCanAccessWatch = when (subscriptionStatus) {
        is SubscriptionStatus.Free,
        null -> false
        is SubscriptionStatus.Plus -> true
    }

    val waitingForSignIn = remember { mutableStateOf(false) }
    if (!userCanAccessWatch) {
        waitingForSignIn.value = true
    }

    val startDestination = if (userCanAccessWatch) WatchListScreen.route else RequirePlusScreen.route

    WearNavScaffold(
        navController = navController,
        startDestination = startDestination,
        state = navState,
    ) {

        scrollable(RequirePlusScreen.route) {
            ScrollToTop.handle(navController, it.scrollableState)
            RequirePlusScreen(
                columnState = it.columnState,
                onContinueToLogin = { navController.navigate(authenticationSubGraph) },
            )
        }

        scrollable(
            route = WatchListScreen.route,
        ) {
            val pagerState = rememberPagerState { NowPlayingPager.pageCount }
            val coroutineScope = rememberCoroutineScope()
            NowPlayingPager(
                navController = navController,
                pagerState = pagerState,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {

                ScrollToTop.handle(navController, it.scrollableState)

                WatchListScreen(
                    columnState = it.columnState,
                    navigateToRoute = navController::navigate,
                    toNowPlaying = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(NowPlayingScreen.pagerIndex)
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
            route = PodcastsScreen.routeHomeFolder,
        ) {
            PodcastsScreenContent(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            )
        }

        scrollable(
            route = PodcastsScreen.routeFolder,
            arguments = listOf(
                navArgument(PodcastsScreen.argumentFolderUuid) {
                    type = NavType.StringType
                }
            ),
        ) {
            PodcastsScreenContent(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            )
        }

        scrollable(
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
                scrollableScaffoldContext = it
            ) {
                PodcastScreen(
                    onEpisodeTap = { episode ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                    },
                    columnState = it.columnState,
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

        scrollable(FiltersScreen.route) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {
                FiltersScreen(
                    onFilterTap = { filterUuid ->
                        navController.navigate(FilterScreen.navigateRoute(filterUuid))
                    },
                    columnState = it.columnState,
                )
            }
        }

        scrollable(
            route = FilterScreen.route,
            arguments = listOf(
                navArgument(FilterScreen.argumentFilterUuid) {
                    type = NavType.StringType
                }
            ),
        ) {
            NowPlayingPager(
                navController = navController,
                swipeToDismissState = swipeToDismissState,
                scrollableScaffoldContext = it,
            ) {
                FilterScreen(
                    onEpisodeTap = { episode ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                    },
                    columnState = it.columnState,
                )
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
                navigateToPrivacySettings = { navController.navigate(PrivacySettingsScreen.route) },
                navigateToAbout = { navController.navigate(WearAboutScreen.route) }
            )
        }

        scrollable(PrivacySettingsScreen.route) {
            PrivacySettingsScreen(scrollState = it.columnState)
        }

        scrollable(WearAboutScreen.route) {
            WearAboutScreen(
                columnState = it.columnState,
                onTermsOfServiceClick = { navController.navigate(UrlScreenRoutes.termsOfService) },
                onPrivacyClick = { navController.navigate(UrlScreenRoutes.privacy) }
            )
        }

        settingsUrlScreens()

        val popToStartDestination: () -> Unit = {
            when (startDestination) {
                WatchListScreen.route -> {
                    val popped = navController.popBackStack(
                        route = WatchListScreen.route,
                        inclusive = false,
                    )
                    if (popped) {
                        ScrollToTop.initiate(navController)
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
    val previousSubscriptionStatus = remember { mutableStateOf<SubscriptionStatus?>(null) }
    when (subscriptionStatus) {
        null -> { /* do nothing */ }
        is SubscriptionStatus.Free -> {
            // This gets the user back to the start destination if they logged in as free. The
            // start destination should have been reset to the RequirePlusScreen already.
            signOut()
            val popped = navController.popBackStack(startDestination, inclusive = false)
            if (popped) {
                ScrollToTop.initiate(navController)
            }
            Toast.makeText(LocalContext.current, LR.string.log_in_with_plus, Toast.LENGTH_LONG).show()
        }
        is SubscriptionStatus.Plus -> {
            if (waitingForSignIn.value &&
                signInState is SignInState.SignedIn &&
                previousSubscriptionStatus.value != subscriptionStatus
            ) {
                navController.navigate(LoggingInScreen.route)
                waitingForSignIn.value = false
            }
        }
    }

    previousSubscriptionStatus.value = subscriptionStatus
}

@Composable
fun PodcastsScreenContent(
    navController: NavHostController,
    swipeToDismissState: SwipeToDismissBoxState,
    scrollableScaffoldContext: ScrollableScaffoldContext,
) {
    NowPlayingPager(
        navController = navController,
        swipeToDismissState = swipeToDismissState,
        scrollableScaffoldContext = scrollableScaffoldContext,
    ) {
        PodcastsScreen(
            columnState = scrollableScaffoldContext.columnState,
            navigateToPodcast = { podcastUuid ->
                navController.navigate(PodcastScreen.navigateRoute(podcastUuid))
            },
            navigateToFolder = { folderUuid ->
                navController.navigate(PodcastsScreen.navigateRoute(folderUuid))
            },
        )
    }
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
