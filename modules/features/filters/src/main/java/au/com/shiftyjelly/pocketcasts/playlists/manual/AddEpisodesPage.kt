package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarStyle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddEpisodesViewModel.PodcastEpisodesUiState
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AddEpisodesPage(
    playlistTitle: String,
    addedEpisodesCount: Int,
    episodeSources: List<ManualPlaylistEpisodeSource>,
    folderPodcastsFlow: (String) -> StateFlow<List<ManualPlaylistPodcastSource>?>,
    episodesFlow: (String) -> StateFlow<PodcastEpisodesUiState?>,
    hasAnyFolders: Boolean,
    useEpisodeArtwork: Boolean,
    onOpenPodcast: () -> Unit,
    onOpenFolder: () -> Unit,
    onAddEpisode: (String) -> Unit,
    onClickNavigationButton: () -> Unit,
    onClickDoneButton: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    searchState: TextFieldState = rememberTextFieldState(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopPageDisplayed = backStackEntry == null || backStackEntry?.destination?.route == AddEpisodesRoutes.HOME

    Column(
        modifier = modifier,
    ) {
        ThemedTopAppBar(
            navigationButton = NavigationButton.CloseBack(isClose = isTopPageDisplayed),
            title = {
                TextH40(
                    text = if (addedEpisodesCount == 0) {
                        stringResource(LR.string.add_to_playlist, playlistTitle)
                    } else {
                        stringResource(
                            LR.string.added_to_playlist,
                            pluralStringResource(LR.plurals.episode_count, addedEpisodesCount, addedEpisodesCount),
                            playlistTitle,
                        )
                    },
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actions = {
                Text(
                    text = stringResource(LR.string.done),
                    fontSize = 17.nonScaledSp,
                    fontWeight = FontWeight(590),
                    color = MaterialTheme.theme.colors.primaryIcon01,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = null,
                            onClick = onClickDoneButton,
                        )
                        .padding(end = 16.dp),
                )
            },
            style = ThemedTopAppBar.Style.Immersive,
            iconColor = MaterialTheme.theme.colors.primaryIcon02,
            windowInsets = WindowInsets(0),
            onNavigationClick = onClickNavigationButton,
        )

        SearchBar(
            state = searchState,
            placeholder = stringResource(LR.string.search),
            style = SearchBarStyle.Small,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        NavHost(
            navController = navController,
            startDestination = AddEpisodesRoutes.HOME,
            enterTransition = { slideInToStart() },
            exitTransition = { slideOutToStart() },
            popEnterTransition = { slideInToEnd() },
            popExitTransition = { slideOutToEnd() },
            modifier = Modifier.weight(1f),
        ) {
            val navigateToSource = { source: ManualPlaylistEpisodeSource ->
                val route = when (source) {
                    is ManualPlaylistFolderSource -> {
                        onOpenFolder()
                        AddEpisodesRoutes.folderRoute(source.uuid)
                    }
                    is ManualPlaylistPodcastSource -> {
                        onOpenPodcast()
                        AddEpisodesRoutes.podcastRoute(source.uuid)
                    }
                }
                navController.navigateOnce(route)
            }

            composable(AddEpisodesRoutes.HOME) {
                AddEpisodeSourcesColumn(
                    sources = episodeSources,
                    noContentData = NoContentData(
                        title = if (hasAnyFolders) {
                            stringResource(LR.string.manual_playlist_search_no_podcast_or_folder_title)
                        } else {
                            stringResource(LR.string.manual_playlist_search_no_podcast_title)
                        },
                        body = if (hasAnyFolders) {
                            stringResource(LR.string.manual_playlist_search_no_podcast_or_folder_body)
                        } else {
                            stringResource(LR.string.manual_playlist_search_no_podcast_body)
                        },
                        iconId = IR.drawable.ic_exclamation_circle,
                    ),
                    onClickSource = navigateToSource,
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                )
            }

            composable(
                AddEpisodesRoutes.FOLDER,
                listOf(navArgument(AddEpisodesRoutes.FOLDER_UUID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                val folderUuid = requireNotNull(arguments.getString(AddEpisodesRoutes.FOLDER_UUID_ARG)) { "Missing folder uuid argument" }
                val podcasts by folderPodcastsFlow(folderUuid).collectAsState()

                AddEpisodeSourcesColumn(
                    sources = podcasts,
                    noContentData = NoContentData(
                        title = stringResource(LR.string.manual_playlist_search_no_podcast_title),
                        body = stringResource(LR.string.manual_playlist_search_no_podcast_body),
                        iconId = IR.drawable.ic_exclamation_circle,
                    ),
                    onClickSource = navigateToSource,
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                )
            }

            composable(
                AddEpisodesRoutes.PODCAST,
                listOf(navArgument(AddEpisodesRoutes.PODCAST_UUID_ARG) { type = NavType.StringType }),
            ) { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments) { "Missing back stack entry arguments" }
                val podcastUuid = requireNotNull(arguments.getString(AddEpisodesRoutes.PODCAST_UUID_ARG)) { "Missing podcast uuid argument" }
                val uiState by episodesFlow(podcastUuid).collectAsState()

                AddEpisodesColumn(
                    uiState = uiState,
                    useEpisodeArtwork = useEpisodeArtwork,
                    onAddEpisode = { episode -> onAddEpisode(episode.uuid) },
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                )
            }
        }
    }
}

internal object AddEpisodesRoutes {
    const val HOME = "home"

    private const val FOLDER_BASE = "folder"
    const val FOLDER_UUID_ARG = "uuid"
    const val FOLDER = "$FOLDER_BASE/{$FOLDER_UUID_ARG}"

    const val PODCAST_BASE = "podcast"
    const val PODCAST_UUID_ARG = "uuid"
    const val PODCAST = "$PODCAST_BASE/{$PODCAST_UUID_ARG}"

    fun folderRoute(uuid: String) = "$FOLDER_BASE/$uuid"

    fun podcastRoute(uuid: String) = "$PODCAST_BASE/$uuid"
}
