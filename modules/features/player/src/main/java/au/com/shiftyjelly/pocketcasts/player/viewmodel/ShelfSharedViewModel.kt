package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShelfSharedViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val analyticsTracker: AnalyticsTracker,
    private val chromeCastAnalytics: ChromeCastAnalytics,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
    private val settings: Settings,
    private val userEpisodeManager: UserEpisodeManager,
    private val transcriptManager: TranscriptManager,
) : ViewModel() {
    private val upNextStateObservable: Observable<UpNextQueue.State> =
        playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(
            episodeManager,
            podcastManager,
        )
            .observeOn(Schedulers.io())

    private val shelfUpNextObservable = upNextStateObservable
        .distinctUntilChanged { t1, t2 ->
            val entry1 = t1 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            val entry2 = t2 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            return@distinctUntilChanged (entry1.episode as? PodcastEpisode)?.isStarred == (entry2.episode as? PodcastEpisode)?.isStarred && entry1.episode.episodeStatus == entry2.episode.episodeStatus && entry1.podcast?.isUsingEffects == entry2.podcast?.isUsingEffects
        }

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    private val _isTranscriptOpen = MutableStateFlow(false)
    val isTranscriptOpen = _isTranscriptOpen.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    val uiState = combine(
        settings.shelfItems.flow,
        shelfUpNextObservable.asFlow(),
        shelfUpNextObservable.asFlow()
            .mapNotNull { state -> (state as? UpNextQueue.State.Loaded)?.episode?.uuid }
            .flatMapLatest { episodeUuid -> transcriptManager.observeIsTranscriptAvailable(episodeUuid) },
        ::createUiState,
    ).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        UiState(),
    )

    private fun createUiState(
        shelfItems: List<ShelfItem>,
        shelfUpNext: UpNextQueue.State,
        isTranscriptAvailable: Boolean,
    ): UiState {
        val episode = (shelfUpNext as? UpNextQueue.State.Loaded)?.episode
        return uiState.value.copy(
            shelfItems = shelfItems.filter { it.showIf(episode) },
            episode = episode,
            isTranscriptAvailable = isTranscriptAvailable,
        )
    }

    fun onEffectsClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Effects, source)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowEffectsOption)
        }
    }

    fun onSleepClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Sleep, source)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowSleepTimerOptions)
        }
    }

    fun onStarClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Star, source)
        playbackManager.upNextQueue.currentEpisode?.let {
            if (it is PodcastEpisode) {
                viewModelScope.launch {
                    episodeManager.toggleStarEpisode(episode = it, SourceView.PLAYER)
                }
            }
        }
    }

    fun onTranscriptClick(
        isTranscriptAvailable: Boolean,
        source: ShelfItemSource,
    ) {
        viewModelScope.launch {
            if (isTranscriptAvailable) {
                trackShelfAction(ShelfItem.Transcript, source)
                openTranscript()
            } else {
                _snackbarMessages.emit(SnackbarMessage.TranscriptNotAvailable)
            }
        }
    }

    fun openTranscript() {
        _isTranscriptOpen.value = true
    }

    fun closeTranscript() {
        _isTranscriptOpen.value = false
    }

    fun onEpisodeDownloadStart(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Download, source)
        viewModelScope.launch {
            _snackbarMessages.emit(SnackbarMessage.EpisodeDownloadStarted)
        }
    }

    fun onShareNotAvailable(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Share, source)
        viewModelScope.launch {
            _snackbarMessages.emit(SnackbarMessage.ShareNotAvailable)
        }
    }

    fun onEpisodeRemoveClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Download, source)
        viewModelScope.launch {
            _snackbarMessages.emit(SnackbarMessage.EpisodeRemoved)
        }
    }

    fun onShareClick(
        podcast: Podcast,
        episode: PodcastEpisode,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.Share, source)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowShareDialog(podcast, episode))
        }
    }

    fun onShowPodcastOrCloudFiles(
        podcast: Podcast?,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.Podcast, source)
        viewModelScope.launch {
            podcast?.let {
                _navigationState.emit(NavigationState.ShowPodcast(podcast))
            } ?: _navigationState.emit(NavigationState.ShowCloudFiles)
        }
    }

    fun onPlayedClick(
        onMarkAsPlayedConfirmed: (episode: BaseEpisode, shouldShuffleUpNext: Boolean) -> Unit,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.Played, source)
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        viewModelScope.launch {
            _navigationState.emit(
                NavigationState.ShowMarkAsPlayedConfirmation(
                    episode,
                ) { onMarkAsPlayedConfirmed(episode, settings.upNextShuffle.value) },
            )
        }
    }

    fun onAddBookmarkClick(
        onboardingUpgradeSource: OnboardingUpgradeSource,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.Bookmark, source)
        viewModelScope.launch {
            val isPaidUser = settings.cachedSubscription.value != null
            if (isPaidUser) {
                _navigationState.emit(NavigationState.ShowAddBookmark)
            } else {
                _navigationState.emit(NavigationState.StartUpsellFlow(onboardingUpgradeSource))
            }
        }
    }

    fun onArchiveClick(
        onArchiveConfirmed: (episode: PodcastEpisode) -> Unit,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.Archive, source)
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        viewModelScope.launch {
            if (episode is PodcastEpisode) {
                _navigationState.emit(
                    NavigationState.ShowPodcastEpisodeArchiveConfirmation(
                        episode,
                    ) { onArchiveConfirmed(episode) },
                )
            } else if (episode is UserEpisode) {
                val deleteState = CloudDeleteHelper.getDeleteState(episode)
                val deleteFunction: (UserEpisode, DeleteState) -> Unit = { ep, delState ->
                    CloudDeleteHelper.deleteEpisode(
                        episode = ep,
                        deleteState = delState,
                        playbackManager = playbackManager,
                        episodeManager = episodeManager,
                        userEpisodeManager = userEpisodeManager,
                        applicationScope = applicationScope,
                    )
                }
                _navigationState.emit(
                    NavigationState.ShowUserEpisodeDeleteConfirmation(
                        episode,
                        deleteState,
                        deleteFunction,
                    ),
                )
            }
        }
    }

    fun onAddToPlaylistClick(
        episodeUuid: String,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.AddToPlaylist, source)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.AddEpisodeToPlaylist(episodeUuid))
        }
    }

    fun onMoreClick() {
        track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_SHOWN)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowMoreActions)
        }
    }

    fun trackShelfAction(
        shelfItem: ShelfItem,
        shelfItemSource: ShelfItemSource,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.PLAYER_SHELF_ACTION_TAPPED,
            mapOf(
                AnalyticsProp.FROM to when (shelfItemSource) {
                    ShelfItemSource.Shelf -> AnalyticsProp.SHELF
                    ShelfItemSource.OverflowMenu -> AnalyticsProp.OVERFLOW_MENU
                },
                AnalyticsProp.ACTION to shelfItem.analyticsValue,
            ),
        )
        if (shelfItem == ShelfItem.Cast) {
            chromeCastAnalytics.trackChromeCastViewShown()
        }
    }

    fun track(event: AnalyticsEvent) {
        analyticsTracker.track(event)
    }

    data class UiState(
        val shelfItems: List<ShelfItem> = emptyList(),
        val episode: BaseEpisode? = null,
        val isTranscriptAvailable: Boolean = false,
    ) {
        val playerShelfItems: List<ShelfItem>
            get() = shelfItems.take(MIN_SHELF_ITEMS_SIZE)
        val playerBottomSheetShelfItems: List<ShelfItem>
            get() = shelfItems.drop(MIN_SHELF_ITEMS_SIZE)
    }

    data class PlayerShelfData(
        val theme: Theme.ThemeType = Theme.ThemeType.DARK,
        val iconTintColor: Int = 0xFFFFFFFF.toInt(),
        val isUserEpisode: Boolean = false,
        val isSleepRunning: Boolean = false,
        val isEffectsOn: Boolean = false,
        val isStarred: Boolean = false,
        val downloadData: DownloadData = DownloadData(),
    ) {
        data class DownloadData(
            val isDownloading: Boolean = false,
            val isQueued: Boolean = false,
            val isDownloaded: Boolean = false,
        )
    }

    sealed interface NavigationState {
        data object ShowEffectsOption : NavigationState
        data object ShowSleepTimerOptions : NavigationState
        data class ShowShareDialog(val podcast: Podcast, val episode: PodcastEpisode) : NavigationState

        data class ShowPodcast(val podcast: Podcast) : NavigationState
        data object ShowCloudFiles : NavigationState
        data class ShowMarkAsPlayedConfirmation(
            val episode: BaseEpisode,
            val onMarkAsPlayedConfirmed: (episode: BaseEpisode) -> Unit,
        ) : NavigationState

        data class ShowPodcastEpisodeArchiveConfirmation(
            val episode: PodcastEpisode,
            val onArchiveConfirmed: (episode: PodcastEpisode) -> Unit,
        ) : NavigationState

        data class ShowUserEpisodeDeleteConfirmation(
            val episode: UserEpisode,
            val deleteState: DeleteState,
            val deleteFunction: (UserEpisode, DeleteState) -> Unit,
        ) : NavigationState

        data object ShowMoreActions : NavigationState
        data object ShowAddBookmark : NavigationState
        data class StartUpsellFlow(val source: OnboardingUpgradeSource) : NavigationState
        data class AddEpisodeToPlaylist(val episodeUuid: String) : NavigationState
    }

    sealed interface SnackbarMessage {
        data object EpisodeDownloadStarted : SnackbarMessage
        data object EpisodeRemoved : SnackbarMessage
        data object TranscriptNotAvailable : SnackbarMessage
        data object ShareNotAvailable : SnackbarMessage
    }

    enum class ShelfItemSource {
        Shelf,
        OverflowMenu,
    }

    companion object {
        const val MIN_SHELF_ITEMS_SIZE = 4

        object AnalyticsProp {
            const val FROM = "from"
            const val ACTION = "action"
            const val SHELF = "shelf"
            const val OVERFLOW_MENU = "overflow_menu"
        }
    }
}
