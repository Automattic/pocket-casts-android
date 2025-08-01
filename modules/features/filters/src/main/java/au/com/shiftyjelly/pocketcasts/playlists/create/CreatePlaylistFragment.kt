package au.com.shiftyjelly.pocketcasts.playlists.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToStart
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.ViewPager2AwareBottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.math.roundToInt
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreatePlaylistFragment : BaseDialogFragment() {
    private val viewModel by viewModels<CreatePlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<CreatePlaylistViewModel.Factory> { factory ->
                factory.create(initialPlaylistName = getString(LR.string.new_playlist))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState by viewModel.uiState.collectAsState()
        var areOtherOptionsExpanded by remember { mutableStateOf(false) }

        DialogBox {
            val navController = rememberNavController()

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
                startDestination = NavigationRoutes.NEW_PLAYLIST,
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(rememberNestedScrollInteropConnection()),
            ) {
                composable(NavigationRoutes.NEW_PLAYLIST) {
                    NewPlaylistPage(
                        titleState = viewModel.playlistNameState,
                        onCreateManualPlaylist = { Timber.i("Create Manual Playlist") },
                        onContinueToSmartPlaylist = {
                            navigateOnce(NavigationRoutes.SMART_PLAYLIST_PREVIEW) {
                                popUpTo(NavigationRoutes.NEW_PLAYLIST) {
                                    inclusive = true
                                }
                            }
                        },
                        onClickClose = ::dismiss,
                    )
                }
                composable(NavigationRoutes.SMART_PLAYLIST_PREVIEW) {
                    SmartPlaylistPreviewPage(
                        playlistTitle = viewModel.playlistNameState.text.toString(),
                        appliedRules = uiState.appliedRules,
                        availableEpisodes = uiState.smartEpisodes,
                        useEpisodeArtwork = uiState.useEpisodeArtwork,
                        areOtherOptionsExpanded = areOtherOptionsExpanded,
                        onCreateSmartPlaylist = { Timber.i("On create smart playlist") },
                        onClickRule = { rule -> navigateOnce(rule.toNavigationRoute()) },
                        toggleOtherOptions = { areOtherOptionsExpanded = !areOtherOptionsExpanded },
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
                        onToggleUnplayedStatus = viewModel::useUnplayedEpisodes,
                        onToggleInProgressStatus = viewModel::useInProgressEpisodes,
                        onToggleCompletedStatus = viewModel::useCompletedEpisodes,
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
                        onToggleConstrainDuration = viewModel::constrainDuration,
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
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetView()
            ?.let { BottomSheetBehavior.from(it) as? ViewPager2AwareBottomSheetBehavior }
            ?.let { behavior ->
                behavior.setPreFlingInterceptor(
                    object : ViewPager2AwareBottomSheetBehavior.PreFlingInterceptor {
                        override fun shouldInterceptFlingGesture(velocityX: Float, velocityY: Float): Boolean {
                            val offsetPx = (view.height * (1f - behavior.calculateSlideOffset())).roundToInt()
                            val offsetDp = offsetPx.pxToDp(requireContext())
                            return offsetDp < 150
                        }

                        override fun onFlingIntercepted(velocityX: Float, velocityY: Float) {
                            view.post {
                                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                            }
                        }
                    },
                )
            }
    }
}

private object NavigationRoutes {
    const val NEW_PLAYLIST = "new_playlist"
    const val SMART_PLAYLIST_PREVIEW = "smart_playlist_preview"
    const val SMART_RULE_PODCASTS = "smart_rule_podcasts"
    const val SMART_RULE_EPISODE_STATUS = "smart_rule_episode_status"
    const val SMART_RULE_RELEASE_DATE = "smart_rule_release_date"
    const val SMART_RULE_EPISODE_DURATION = "smart_rule_episode_duration"
    const val SMART_RULE_DOWNLOAD_STATUS = "smart_rule_download_status"
    const val SMART_RULE_MEDIA_TYPE = "smart_rule_media_type"
}

private fun RuleType.toNavigationRoute() = when (this) {
    RuleType.Podcasts -> NavigationRoutes.SMART_RULE_PODCASTS
    RuleType.EpisodeStatus -> NavigationRoutes.SMART_RULE_EPISODE_STATUS
    RuleType.ReleaseDate -> NavigationRoutes.SMART_RULE_RELEASE_DATE
    RuleType.EpisodeDuration -> NavigationRoutes.SMART_RULE_EPISODE_DURATION
    RuleType.DownloadStatus -> NavigationRoutes.SMART_RULE_DOWNLOAD_STATUS
    RuleType.MediaType -> NavigationRoutes.SMART_RULE_MEDIA_TYPE
    RuleType.Starred -> NavigationRoutes.SMART_PLAYLIST_PREVIEW
}
