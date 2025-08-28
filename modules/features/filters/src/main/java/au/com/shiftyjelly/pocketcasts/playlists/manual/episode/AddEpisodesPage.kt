package au.com.shiftyjelly.pocketcasts.playlists.manual.episode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import timber.log.Timber

@Composable
internal fun AddEpisodesPage(
    episodeSources: List<ManualPlaylistEpisodeSource>,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AddEpisodesRoutes.HOME,
        enterTransition = { slideInToStart() },
        exitTransition = { slideOutToStart() },
        popEnterTransition = { slideInToEnd() },
        popExitTransition = { slideOutToEnd() },
        modifier = modifier,
    ) {
        val navigateToSource = { source: ManualPlaylistEpisodeSource ->
            when (source) {
                is ManualPlaylistFolderSource -> {
                    navController.navigateOnce(AddEpisodesRoutes.folderRoute(source.uuid))
                }

                is ManualPlaylistPodcastSource -> {
                    Timber.i("Go to podcast: ${source.title}")
                }
            }
        }

        composable(AddEpisodesRoutes.HOME) {
            EpisodeSourcesColumn(
                sources = episodeSources,
                onClickSource = navigateToSource,
            )
        }

        composable(
            AddEpisodesRoutes.FOLDER,
            listOf(navArgument(AddEpisodesRoutes.FOLDER_UUID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
            val folderUuid = requireNotNull(arguments.getString(AddEpisodesRoutes.FOLDER_UUID_ARG)) { "Missing folder uuid argument" }
            val podcasts = remember(folderUuid) {
                episodeSources.filterIsInstance<ManualPlaylistFolderSource>().find { it.uuid == folderUuid }?.podcastSources.orEmpty()
            }
            EpisodeSourcesColumn(
                sources = podcasts,
                onClickSource = navigateToSource,
            )
        }
    }
}

private object AddEpisodesRoutes {
    const val HOME = "home"

    private const val FOLDER_BASE = "folder"
    const val FOLDER_UUID_ARG = "uuid"
    const val FOLDER = "$FOLDER_BASE/{$FOLDER_UUID_ARG}"

    fun folderRoute(uuid: String) = "$FOLDER_BASE/$uuid"
}
