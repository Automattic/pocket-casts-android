package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.R
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
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.settingsRoutes
import com.google.android.horologist.compose.layout.AppScaffold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
                    email = state.email,
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
    email: String?,
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
        null,
        -> false

        is SubscriptionStatus.Paid -> true
    }

    val waitingForSignIn = remember { mutableStateOf(false) }
    if (!userCanAccessWatch) {
        waitingForSignIn.value = true
    }

    val startDestination = if (userCanAccessWatch) WatchListScreen.route else RequirePlusScreen.route

    AppScaffold {
        SwipeDismissableNavHost(
            startDestination = startDestination,
            navController = navController,
            state = navState,
        ) {
            composable(
                route = RequirePlusScreen.route,
            ) {
                RequirePlusScreen(
                    onContinueToLogin = { navController.navigate(authenticationSubGraph) },
                )
            }

            composable(
                route = WatchListScreen.route,
            ) {
                NowPlayingPager(
                    allowSwipeToDismiss = false,
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    val scope = rememberCoroutineScope()
                    WatchListScreen(
                        columnState = columnState,
                        navigateToRoute = navController::navigate,
                        toNowPlaying = {
                            scope.launch {
                                pagerState.animateScrollToPage(NowPlayingScreen.pagerIndex)
                            }
                        },
                    )
                }
            }

            composable(
                route = PCVolumeScreen.route,
            ) {
                PCVolumeScreen()
            }

            composable(
                route = StreamingConfirmationScreen.route,
            ) {
                StreamingConfirmationScreen(
                    onFinished = { result ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            StreamingConfirmationScreen.resultKey,
                            result,
                        )
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = PodcastsScreen.routeHomeFolder,
            ) {
                PodcastsScreenContent(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                )
            }

            composable(
                route = PodcastsScreen.routeFolder,
                arguments = listOf(
                    navArgument(PodcastsScreen.argumentFolderUuid) {
                        type = NavType.StringType
                    },
                ),
            ) {
                PodcastsScreenContent(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                )
            }

            composable(
                route = PodcastScreen.route,
                arguments = listOf(
                    navArgument(PodcastScreen.argument) {
                        type = NavType.StringType
                    },
                ),
            ) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    PodcastScreen(
                        columnState = columnState,
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
                    FiltersScreen(
                        onFilterTap = { filterUuid ->
                            navController.navigate(FilterScreen.navigateRoute(filterUuid))
                        },
                        columnState = columnState,
                    )
                }
            }

            composable(
                route = FilterScreen.route,
                arguments = listOf(
                    navArgument(FilterScreen.argumentFilterUuid) {
                        type = NavType.StringType
                    },
                ),
            ) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    FilterScreen(
                        onEpisodeTap = { episode ->
                            navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                        },
                        columnState = columnState,
                    )
                }
            }

            composable(DownloadsScreen.route) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    DownloadsScreen(
                        columnState = columnState,
                        onItemClick = { episode ->
                            val route = EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid)
                            navController.navigate(route)
                        },
                    )
                }
            }

            composable(FilesScreen.route) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    FilesScreen(
                        columnState = columnState,
                        navigateToEpisode = { episodeUuid ->
                            navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid))
                        },
                    )
                }
            }

            settingsRoutes(navController)

            authenticationNavGraph(
                navController = navController,
                onEmailSignInSuccess = {
                    navController.navigate(LoggingInScreen.route)
                },
                googleSignInSuccessScreen = { googleSignInAccount ->
                    LoggingInScreen(
                        avatarUrl = googleSignInAccount?.photoUrl?.toString(),
                        name = googleSignInAccount?.givenName,
                        onClose = {},
                    )
                },
            )

            loggingInScreens(
                onClose = {
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
                },
            )

            composable(
                route = PCVolumeScreen.route,
            ) {
                PCVolumeScreen()
            }

            composable(
                route = EffectsScreen.route,
            ) {
                EffectsScreen()
            }
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
        null -> {
            /* do nothing */
        }

        is SubscriptionStatus.Free -> {
            // This gets the user back to the start destination if they logged in as free. The
            // start destination should have been reset to the RequirePlusScreen already.
            signOut()
            val popped = navController.popBackStack(startDestination, inclusive = false)
            if (popped) {
                ScrollToTop.initiate(navController)
            }
            val message = if (email != null) {
                stringResource(R.string.log_in_free_acccount, email)
            } else {
                stringResource(R.string.log_in_with_plus)
            }
            Toast.makeText(LocalContext.current, message, Toast.LENGTH_LONG).show()
        }

        is SubscriptionStatus.Paid -> {
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
) {
    NowPlayingPager(
        navController = navController,
        swipeToDismissState = swipeToDismissState,
    ) {
        PodcastsScreen(
            columnState = columnState,
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

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun DefaultPreview() {
    WearApp(
        email = "",
        signInState = null,
        subscriptionStatus = null,
        showLoggingInScreen = false,
        onLoggingInScreenShown = {},
        signOut = {},
    )
}
