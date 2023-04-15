package au.com.shiftyjelly.pocketcasts.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.Icon
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
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
import au.com.shiftyjelly.pocketcasts.wear.ui.episode.NotificationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: WearMainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            WearApp(
                themeType = theme.activeTheme,
                signInConfirmationAction = state.signInConfirmationAction,
                onSignInConfirmationActionHandled = viewModel::onSignInConfirmationActionHandled,
            )
        }
    }
}

private object Routes {
    const val signedInNotificationScreen = "signedInNotificationScreen"
}

@Composable
fun WearApp(
    themeType: Theme.ThemeType,
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
) {
    WearAppTheme(themeType) {

        val navController = rememberSwipeDismissableNavController()

        WearNavScaffold(
            navController = navController,
            startDestination = WatchListScreen.route
        ) {

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
            ) {
                PodcastsScreen(
                    listState = it.scrollableState,
                    navigateToPodcast = { podcastUuid ->
                        navController.navigate(PodcastScreen.navigateRoute(podcastUuid))
                    }
                )
            }

            composable(
                route = PodcastScreen.route,
                arguments = listOf(
                    navArgument(PodcastScreen.argument) {
                        type = NavType.StringType
                    }
                ),
            ) {
                PodcastScreen(
                    onEpisodeTap = { episode ->
                        navController.navigate(EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid))
                    },
                )
            }

            episodeGraph(
                navigateToPodcast = { podcastUuid ->
                    navController.navigate(PodcastScreen.navigateRoute(podcastUuid))
                },
                navController = navController,
            )

            composable(FiltersScreen.route) { FiltersScreen() }

            scrollable(DownloadsScreen.route) {
                DownloadsScreen(
                    columnState = it.columnState,
                    onItemClick = { episode ->
                        val route = EpisodeScreenFlow.navigateRoute(episodeUuid = episode.uuid)
                        navController.navigate(route)
                    }
                )
            }

            composable(FilesScreen.route) { FilesScreen() }

            scrollable(SettingsScreen.route) {
                SettingsScreen(
                    scrollState = it.columnState,
                    signInClick = { navController.navigate(authenticationSubGraph) },
                )
            }

            authenticationGraph(navController)

            composable(Routes.signedInNotificationScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
                NotificationScreen(
                    text = stringResource(LR.string.profile_logging_in),
                    closeAfterDuration = null,
                    onClose = { navController.popBackStack() },
                    icon = {
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        Icon(
                            painter = painterResource(IR.drawable.ic_retry),
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                            }
                        )
                    }
                )
            }
        }
    }
}

private fun handleSignInConfirmation(
    signInConfirmationAction: SignInConfirmationAction?,
    onSignInConfirmationActionHandled: () -> Unit,
    navController: NavController,
) {

    val signInNotificationShowing = navController.currentDestination?.route == Routes.signedInNotificationScreen

    when (signInConfirmationAction) {
        SignInConfirmationAction.Show -> {
            if (!signInNotificationShowing) {
                navController.navigate(Routes.signedInNotificationScreen)
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
        themeType = Theme.ThemeType.DARK,
        signInConfirmationAction = null,
        onSignInConfirmationActionHandled = {},
    )
}
