package au.com.shiftyjelly.pocketcasts.playlists.edit

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToStart
import au.com.shiftyjelly.pocketcasts.playlists.rules.AppliedRulesPage
import au.com.shiftyjelly.pocketcasts.playlists.rules.DownloadStatusRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.EpisodeDurationRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.EpisodeStatusRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.MediaTypeRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.PodcastsRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.ReleaseDateRulePage
import au.com.shiftyjelly.pocketcasts.playlists.rules.RuleType
import au.com.shiftyjelly.pocketcasts.playlists.rules.StarredRulePage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class SmartRulesEditFragment : BaseDialogFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<SmartRulesEditViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SmartRulesEditViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox {
            val navController = rememberNavController()
            ClearTransientRulesStateEffect(navController)

            AnimatedNonNullVisibility(
                item = viewModel.uiState.collectAsState().value,
                enter = fadeIn,
                exit = fadeOut,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(rememberNestedScrollInteropConnection()),
            ) { uiState ->
                fun goBackToPlaylistPreview() {
                    navController.popBackStack(NavigationRoutes.SMART_PLAYLIST_PREVIEW, inclusive = false)
                }

                fun navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
                    if (navController.currentDestination?.route != route) {
                        navController.navigate(route, builder)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = NavigationRoutes.SMART_PLAYLIST_PREVIEW,
                    enterTransition = { slideInToStart() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInToEnd() },
                    popExitTransition = { slideOutToEnd() },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(rememberNestedScrollInteropConnection()),
                ) {
                    composable(NavigationRoutes.SMART_PLAYLIST_PREVIEW) {
                        AppliedRulesPage(
                            playlistTitle = uiState.playlistTitle,
                            appliedRules = uiState.appliedRules,
                            availableEpisodes = uiState.smartEpisodes,
                            totalEpisodeCount = uiState.totalEpisodeCount,
                            useEpisodeArtwork = uiState.useEpisodeArtwork,
                            onClickRule = { rule -> navigateOnce(rule.toNavigationRoute()) },
                            onClickClose = ::dismiss,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_PODCASTS) {
                        PodcastsRulePage(
                            useAllPodcasts = uiState.rulesBuilder.useAllPodcasts,
                            selectedPodcastUuids = uiState.rulesBuilder.selectedPodcasts,
                            podcasts = uiState.followedPodcasts,
                            onToggleAllPodcasts = viewModel::useAllPodcasts,
                            onSelectPodcast = viewModel::selectPodcast,
                            onDeselectPodcast = viewModel::deselectPodcast,
                            onSelectAllPodcasts = viewModel::selectAllPodcasts,
                            onDeselectAllPodcasts = viewModel::deselectAllPodcasts,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.Podcasts)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_EPISODE_STATUS) {
                        EpisodeStatusRulePage(
                            rule = uiState.rulesBuilder.episodeStatusRule,
                            onChangeUnplayedStatus = viewModel::useUnplayedEpisodes,
                            onChangeInProgressStatus = viewModel::useInProgressEpisodes,
                            onChangeCompletedStatus = viewModel::useCompletedEpisodes,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.EpisodeStatus)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_RELEASE_DATE) {
                        ReleaseDateRulePage(
                            selectedRule = uiState.rulesBuilder.releaseDateRule,
                            onSelectReleaseDate = viewModel::useReleaseDate,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.ReleaseDate)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_EPISODE_DURATION) {
                        EpisodeDurationRulePage(
                            isDurationConstrained = uiState.rulesBuilder.isEpisodeDurationConstrained,
                            minDuration = uiState.rulesBuilder.minEpisodeDuration,
                            maxDuration = uiState.rulesBuilder.maxEpisodeDuration,
                            onChangeConstrainDuration = viewModel::useConstrainedDuration,
                            onDecrementMinDuration = viewModel::decrementMinDuration,
                            onIncrementMinDuration = viewModel::incrementMinDuration,
                            onDecrementMaxDuration = viewModel::decrementMaxDuration,
                            onIncrementMaxDuration = viewModel::incrementMaxDuration,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.EpisodeDuration)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_DOWNLOAD_STATUS) {
                        DownloadStatusRulePage(
                            selectedRule = uiState.rulesBuilder.downloadStatusRule,
                            onSelectDownloadStatus = viewModel::useDownloadStatus,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.DownloadStatus)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_MEDIA_TYPE) {
                        MediaTypeRulePage(
                            selectedRule = uiState.rulesBuilder.mediaTypeRule,
                            onSelectMediaType = viewModel::useMediaType,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.MediaType)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                    composable(NavigationRoutes.SMART_RULE_STARRED) {
                        StarredRulePage(
                            selectedRule = uiState.rulesBuilder.starredRule,
                            starredEpisodes = uiState.smartStarredEpisodes,
                            useEpisodeArtwork = uiState.useEpisodeArtwork,
                            onChangeUseStarredEpisodes = viewModel::useStarredEpisodes,
                            onSaveRule = {
                                viewModel.applyRule(RuleType.Starred)
                                goBackToPlaylistPreview()
                            },
                            onClickBack = ::goBackToPlaylistPreview,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearTransientRulesStateEffect(navController: NavController) {
        var currentRoute by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { entry ->
                val newRoute = entry.destination.route
                if (currentRoute != newRoute) {
                    currentRoute = newRoute
                    viewModel.clearTransientRules()
                }
            }
        }
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartRulesEditFragmentArgs"

        fun newInstance(playlistUuid: String) = SmartRulesEditFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()

private object NavigationRoutes {
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
    RuleType.Podcasts -> NavigationRoutes.SMART_RULE_PODCASTS
    RuleType.EpisodeStatus -> NavigationRoutes.SMART_RULE_EPISODE_STATUS
    RuleType.ReleaseDate -> NavigationRoutes.SMART_RULE_RELEASE_DATE
    RuleType.EpisodeDuration -> NavigationRoutes.SMART_RULE_EPISODE_DURATION
    RuleType.DownloadStatus -> NavigationRoutes.SMART_RULE_DOWNLOAD_STATUS
    RuleType.MediaType -> NavigationRoutes.SMART_RULE_MEDIA_TYPE
    RuleType.Starred -> NavigationRoutes.SMART_RULE_STARRED
}
