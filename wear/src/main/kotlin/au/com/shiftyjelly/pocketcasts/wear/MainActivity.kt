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
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.FilesScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.LoggingInScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.ScrollToTop
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.AUTHENTICATION_SUB_GRAPH
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.RequirePlusScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.authenticationNavGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingPager
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.EpisodeScreenFlow.episodeGraph
import au.com.shiftyjelly.pocketcasts.wear.ui.player.EffectsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.PCVolumeScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.playlist.PlaylistScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.playlists.PlaylistsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.settingsRoutes
import com.google.android.horologist.compose.layout.AppScaffold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                    showLoggingInScreen = state.showLoggingInScreen,
                    onShowLoginScreen = viewModel::onSignInConfirmationActionHandled,
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
private fun WearApp(
    signInState: SignInState,
    showLoggingInScreen: Boolean,
    onShowLoginScreen: () -> Unit,
    signOut: () -> Unit,
) {
    val navController = rememberSwipeDismissableNavController()
    val swipeToDismissState = rememberSwipeToDismissBoxState()
    val navState = rememberSwipeDismissableNavHostState(swipeToDismissState)

    if (showLoggingInScreen) {
        navController.navigate(LoggingInScreen.ROUTE_WITH_DELAY)
        onShowLoginScreen()
    }

    val userCanAccessWatch = signInState.isSignedInAsPlusOrPatron

    val waitingForSignIn = remember { mutableStateOf(false) }
    if (!userCanAccessWatch) {
        waitingForSignIn.value = true
    }

    val startDestination = if (userCanAccessWatch) WatchListScreen.ROUTE else RequirePlusScreen.ROUTE

    AppScaffold {
        SwipeDismissableNavHost(
            startDestination = startDestination,
            navController = navController,
            state = navState,
        ) {
            composable(
                route = RequirePlusScreen.ROUTE,
            ) {
                RequirePlusScreen(
                    onContinueToLogin = { navController.navigate(AUTHENTICATION_SUB_GRAPH) },
                )
            }

            composable(
                route = WatchListScreen.ROUTE,
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
                                pagerState.animateScrollToPage(NowPlayingScreen.PAGER_INDEX)
                            }
                        },
                    )
                }
            }

            composable(
                route = PCVolumeScreen.ROUTE,
            ) {
                PCVolumeScreen()
            }

            composable(
                route = StreamingConfirmationScreen.ROUTE,
            ) {
                StreamingConfirmationScreen(
                    onFinish = { result ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            StreamingConfirmationScreen.RESULT_KEY,
                            result,
                        )
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = PodcastsScreen.ROUTE_HOME_FOLDER,
            ) {
                PodcastsScreenContent(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                )
            }

            composable(
                route = PodcastsScreen.ROUTE_FOLDER,
                arguments = listOf(
                    navArgument(PodcastsScreen.ARGUMENT_FOLDER_UUID) {
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
                route = PodcastScreen.ROUTE,
                arguments = listOf(
                    navArgument(PodcastScreen.ARGUMENT) {
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

            composable(PlaylistsScreen.ROUTE) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    PlaylistsScreen(
                        onClickPlaylist = { playlist ->
                            navController.navigate(PlaylistScreen.navigateRoute(playlist.uuid, playlist.type))
                        },
                        columnState = columnState,
                    )
                }
            }

            composable(
                route = PlaylistScreen.ROUTE,
                arguments = listOf(
                    navArgument(PlaylistScreen.ARGUMENT_PLAYLIST_UUID) {
                        type = NavType.StringType
                    },
                    navArgument(PlaylistScreen.ARGUMENT_PLAYLIST_TYPE) {
                        type = NavType.StringType
                    },
                ),
            ) {
                NowPlayingPager(
                    navController = navController,
                    swipeToDismissState = swipeToDismissState,
                ) {
                    PlaylistScreen(
                        onEpisodeTap = { episode ->
                            navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                        },
                        columnState = columnState,
                    )
                }
            }

            composable(DownloadsScreen.ROUTE) {
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

            composable(FilesScreen.ROUTE) {
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
                    navController.navigate(LoggingInScreen.ROUTE)
                },
                googleSignInSuccessScreen = { googleAccount ->
                    LoggingInScreen(
                        avatarUrl = googleAccount.avatarUrl,
                        name = googleAccount.name,
                        onClose = {},
                    )
                },
            )

            loggingInScreens(
                onClose = {
                    when (startDestination) {
                        WatchListScreen.ROUTE -> {
                            val popped = navController.popBackStack(
                                route = WatchListScreen.ROUTE,
                                inclusive = false,
                            )
                            if (popped) {
                                ScrollToTop.initiate(navController)
                            }
                        }

                        RequirePlusScreen.ROUTE -> {
                            navController.popBackStack(
                                route = RequirePlusScreen.ROUTE,
                                inclusive = false,
                            )
                        }

                        else -> throw IllegalStateException("Unexpected start destination $startDestination")
                    }
                },
            )

            composable(
                route = PCVolumeScreen.ROUTE,
            ) {
                PCVolumeScreen()
            }

            composable(
                route = EffectsScreen.ROUTE,
            ) {
                EffectsScreen()
            }
        }
    }

    when (signInState) {
        is SignInState.SignedOut -> Unit // Do nothing

        is SignInState.SignedIn -> {
            val subscription = signInState.subscription
            if (subscription == null) {
                // This gets the user back to the start destination if they logged in as free. The
                // start destination should have been reset to the RequirePlusScreen already.
                signOut()
                val popped = navController.popBackStack(startDestination, inclusive = false)
                if (popped) {
                    ScrollToTop.initiate(navController)
                }
                val message = stringResource(LR.string.log_in_free_acccount, signInState.email)
                Toast.makeText(LocalContext.current, message, Toast.LENGTH_LONG).show()
            } else if (waitingForSignIn.value) {
                navController.navigate(LoggingInScreen.ROUTE)
                waitingForSignIn.value = false
            }
        }
    }
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
    composable(LoggingInScreen.ROUTE) {
        LoggingInScreen(onClose = onClose)
    }

    composable(LoggingInScreen.ROUTE_WITH_DELAY) {
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
        signInState = SignInState.SignedOut,
        showLoggingInScreen = false,
        onShowLoginScreen = {},
        signOut = {},
    )
}
