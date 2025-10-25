package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule

@Composable
internal fun ManageSmartRulesPage(
    playlistName: String,
    appliedRules: AppliedRules,
    rulesBuilder: RulesBuilder,
    smartEpisodes: List<PlaylistEpisode.Available>,
    smartStarredEpisodes: List<PlaylistEpisode.Available>,
    followedPodcasts: List<Podcast>,
    starredEpisodeCount: Int,
    useEpisodeArtwork: Boolean,
    navController: NavHostController,
    listener: ManageSmartRulesListener,
    modifier: Modifier = Modifier,
    startDestination: String = ManageSmartRulesRoutes.SMART_PLAYLIST_PREVIEW,
    isPlaylistPreviewShown: Boolean = true,
    podcastSearchState: TextFieldState = rememberTextFieldState(),
    builder: NavGraphBuilder.() -> Unit = {},
) {
    var areOtherOptionsExpanded by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopPageDisplayed = backStackEntry == null ||
        backStackEntry?.destination?.route == startDestination ||
        backStackEntry?.destination?.route == ManageSmartRulesRoutes.SMART_PLAYLIST_PREVIEW

    fun goBackToPreview() {
        navController.popBackStack(ManageSmartRulesRoutes.SMART_PLAYLIST_PREVIEW, inclusive = false)
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ThemedTopAppBar(
            navigationButton = NavigationButton.CloseBack(isClose = isTopPageDisplayed),
            style = ThemedTopAppBar.Style.Immersive,
            actions = {
                if (backStackEntry?.destination?.route == ManageSmartRulesRoutes.SMART_RULE_PODCASTS) {
                    PodcastRulesActions(
                        useAllPodcasts = rulesBuilder.useAllPodcasts,
                        selectedPodcastUuids = rulesBuilder.selectedPodcasts,
                        podcasts = followedPodcasts,
                        onSelectAllPodcasts = listener::onSelectAllPodcasts,
                        onDeselectAllPodcasts = listener::onDeselectAllPodcasts,
                    )
                }
            },
            iconColor = MaterialTheme.theme.colors.primaryIcon03,
            windowInsets = WindowInsets(0),
            onNavigationClick = {
                if (isTopPageDisplayed) {
                    listener.onClose()
                } else {
                    goBackToPreview()
                }
            },
        )

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { slideInToStart() },
            exitTransition = { slideOutToStart() },
            popEnterTransition = { slideInToEnd() },
            popExitTransition = { slideOutToEnd() },
        ) {
            builder()
            composable(ManageSmartRulesRoutes.SMART_PLAYLIST_PREVIEW) {
                AppliedRulesPage(
                    playlistName = playlistName,
                    appliedRules = appliedRules,
                    availableEpisodes = smartEpisodes,
                    starredEpisodeCount = starredEpisodeCount,
                    useEpisodeArtwork = useEpisodeArtwork,
                    areOtherOptionsExpanded = areOtherOptionsExpanded,
                    isPlaylistPreviewShown = isPlaylistPreviewShown,
                    onCreatePlaylist = listener.createPlaylistCallback(),
                    onClickRule = { rule -> navController.navigateOnce(rule.toNavigationRoute()) },
                    toggleOtherOptions = { areOtherOptionsExpanded = !areOtherOptionsExpanded },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_PODCASTS) {
                PodcastsRulePage(
                    useAllPodcasts = rulesBuilder.useAllPodcasts,
                    selectedPodcastUuids = rulesBuilder.selectedPodcasts,
                    podcasts = followedPodcasts,
                    searchState = podcastSearchState,
                    onChangeUseAllPodcasts = listener::onChangeUseAllPodcasts,
                    onSelectPodcast = listener::onSelectPodcast,
                    onDeselectPodcast = listener::onDeselectPodcast,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.Podcasts)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_EPISODE_STATUS) {
                EpisodeStatusRulePage(
                    rule = rulesBuilder.episodeStatusRule,
                    onChangeUnplayedStatus = listener::onChangeUnplayedStatus,
                    onChangeInProgressStatus = listener::onChangeInProgressStatus,
                    onChangeCompletedStatus = listener::onChangeCompletedStatus,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.EpisodeStatus)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_RELEASE_DATE) {
                ReleaseDateRulePage(
                    selectedRule = rulesBuilder.releaseDateRule,
                    onSelectReleaseDate = listener::onUseReleaseDate,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.ReleaseDate)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_EPISODE_DURATION) {
                EpisodeDurationRulePage(
                    isDurationConstrained = rulesBuilder.isEpisodeDurationConstrained,
                    minDuration = rulesBuilder.minEpisodeDuration,
                    maxDuration = rulesBuilder.maxEpisodeDuration,
                    onChangeConstrainDuration = listener::onUseConstrainedDuration,
                    onDecrementMinDuration = listener::onDecrementMinDuration,
                    onIncrementMinDuration = listener::onIncrementMinDuration,
                    onDecrementMaxDuration = listener::onDecrementMaxDuration,
                    onIncrementMaxDuration = listener::onIncrementMaxDuration,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.EpisodeDuration)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_DOWNLOAD_STATUS) {
                DownloadStatusRulePage(
                    selectedRule = rulesBuilder.downloadStatusRule,
                    onSelectDownloadStatus = listener::onUseDownloadStatus,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.DownloadStatus)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_MEDIA_TYPE) {
                MediaTypeRulePage(
                    selectedRule = rulesBuilder.mediaTypeRule,
                    onSelectMediaType = listener::onUseMediaType,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.MediaType)
                        goBackToPreview()
                    },
                )
            }
            composable(ManageSmartRulesRoutes.SMART_RULE_STARRED) {
                StarredRulePage(
                    selectedRule = rulesBuilder.starredRule,
                    starredEpisodes = smartStarredEpisodes,
                    useEpisodeArtwork = useEpisodeArtwork,
                    onChangeUseStarredEpisodes = listener::onUseStarredEpisodes,
                    onSaveRule = {
                        listener.onApplyRule(RuleType.Starred)
                        goBackToPreview()
                    },
                )
            }
        }
    }
}

internal interface ManageSmartRulesListener {
    fun onChangeUseAllPodcasts(shouldUse: Boolean)

    fun onSelectPodcast(uuid: String)

    fun onDeselectPodcast(uuid: String)

    fun onSelectAllPodcasts()

    fun onDeselectAllPodcasts()

    fun onChangeUnplayedStatus(shouldUse: Boolean)

    fun onChangeInProgressStatus(shouldUse: Boolean)

    fun onChangeCompletedStatus(shouldUse: Boolean)

    fun onUseReleaseDate(releaseDate: ReleaseDateRule)

    fun onUseConstrainedDuration(shouldUse: Boolean)

    fun onDecrementMinDuration()

    fun onIncrementMinDuration()

    fun onDecrementMaxDuration()

    fun onIncrementMaxDuration()

    fun onUseDownloadStatus(downloadStatus: DownloadStatusRule)

    fun onUseMediaType(mediaType: MediaTypeRule)

    fun onUseStarredEpisodes(shouldUse: Boolean)

    fun onApplyRule(rule: RuleType)

    fun createPlaylistCallback(): (() -> Unit)?

    fun onClose()
}

internal object ManageSmartRulesRoutes {
    const val SMART_PLAYLIST_PREVIEW = "smart_playlist_preview"
    const val SMART_RULE_PODCASTS = "smart_rule_podcasts"
    const val SMART_RULE_EPISODE_STATUS = "smart_rule_episode_status"
    const val SMART_RULE_RELEASE_DATE = "smart_rule_release_date"
    const val SMART_RULE_EPISODE_DURATION = "smart_rule_episode_duration"
    const val SMART_RULE_DOWNLOAD_STATUS = "smart_rule_download_status"
    const val SMART_RULE_MEDIA_TYPE = "smart_rule_media_type"
    const val SMART_RULE_STARRED = "smart_rule_starred"
}

private fun RuleType.toNavigationRoute() = when (this) {
    RuleType.Podcasts -> ManageSmartRulesRoutes.SMART_RULE_PODCASTS
    RuleType.EpisodeStatus -> ManageSmartRulesRoutes.SMART_RULE_EPISODE_STATUS
    RuleType.ReleaseDate -> ManageSmartRulesRoutes.SMART_RULE_RELEASE_DATE
    RuleType.EpisodeDuration -> ManageSmartRulesRoutes.SMART_RULE_EPISODE_DURATION
    RuleType.DownloadStatus -> ManageSmartRulesRoutes.SMART_RULE_DOWNLOAD_STATUS
    RuleType.MediaType -> ManageSmartRulesRoutes.SMART_RULE_MEDIA_TYPE
    RuleType.Starred -> ManageSmartRulesRoutes.SMART_RULE_STARRED
}
