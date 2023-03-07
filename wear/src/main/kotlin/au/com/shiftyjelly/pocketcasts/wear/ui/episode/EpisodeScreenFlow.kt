package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable

object EpisodeScreenFlow {
    const val episodeUuidArgument = "episodeUuid"
    const val route = "episode/{$episodeUuidArgument}"
    fun navigateRoute(episodeUuid: String) = "episode/$episodeUuid"

    // Routes
    private const val episodeScreen = "episodeScreen"
    private const val upNextOptionsScreen = "upNextOptionsScreen"

    fun NavGraphBuilder.episodeFlowGraph(
        navigateToPodcast: (podcastUuid: String) -> Unit,
        navController: NavController,
    ) {
        navigation(
            route = this@EpisodeScreenFlow.route,
            startDestination = episodeScreen,
            arguments = listOf(
                navArgument(episodeUuidArgument) {
                    type = NavType.StringType
                }
            ),
        ) {
            scrollable(
                route = episodeScreen,
                columnStateFactory = ScalingLazyColumnDefaults.belowTimeText(
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top)
                )
            ) {
                EpisodeScreen(
                    columnState = it.columnState,
                    navigateToPodcast = navigateToPodcast,
                    navigateToUpNextOptions = { navController.navigate(upNextOptionsScreen) },
                )
            }

            composable(upNextOptionsScreen) {
                it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off
                UpNextOptionsScreen(
                    episodeScreenViewModelStoreOwner =
                    navController.getBackStackEntry(episodeScreen), // Reuse view model from EpisodeScreen
                    onComplete = { navController.popBackStack() },
                )
            }
        }
    }
}
