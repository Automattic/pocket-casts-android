package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.firstHlsStreamUrl
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SelectedStream
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.AlternateEnclosureManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlayerShelfActionTappedEvent
import com.automattic.eventhorizon.PlayerShelfOverflowMenuShownEvent
import com.automattic.eventhorizon.ShelfActionSourceType
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShelfSharedViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val eventHorizon: EventHorizon,
    private val chromeCastAnalytics: ChromeCastAnalytics,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
    private val settings: Settings,
    private val userEpisodeManager: UserEpisodeManager,
    private val transcriptManager: TranscriptManager,
    private val downloadQueue: DownloadQueue,
    private val alternateEnclosureManager: AlternateEnclosureManager,
) : ViewModel() {
    private val upNextStateObservable: Observable<UpNextQueue.State> =
        playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(
            episodeManager,
            podcastManager,
        )
            .observeOn(Schedulers.io())

    private val shelfUpNextObservable = upNextStateObservable
        .distinctUntilChanged { oldState, newState ->
            val oldLoaded = oldState as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            val newLoaded = newState as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
            val oldPodcastEpisode = oldLoaded.episode as? PodcastEpisode
            val newPodcastEpisode = newLoaded.episode as? PodcastEpisode

            oldPodcastEpisode?.uuid == newPodcastEpisode?.uuid &&
                oldPodcastEpisode?.isStarred == newPodcastEpisode?.isStarred &&
                oldLoaded.episode.downloadStatus == newLoaded.episode.downloadStatus &&
                oldLoaded.podcast?.isUsingEffects == newLoaded.podcast?.isUsingEffects
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
        playbackManager.selectedStreams,
        shelfUpNextObservable.asFlow()
            .mapNotNull { state -> (state as? UpNextQueue.State.Loaded)?.episode?.uuid }
            .flatMapLatest { episodeUuid -> alternateEnclosureManager.observeForEpisode(episodeUuid) },
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
        selectedStreams: Map<String, SelectedStream>,
        alternateEnclosures: List<EpisodeAlternateEnclosure>,
    ): UiState {
        val episode = (shelfUpNext as? UpNextQueue.State.Loaded)?.episode
        val hlsStreamUrl = alternateEnclosures.firstHlsStreamUrl()
        val streamToggleEnabled = FeatureFlag.isEnabled(Feature.STREAM_SELECTOR) && FeatureFlag.isEnabled(Feature.HLS_STREAMING)
        val canStreamToggle = streamToggleEnabled &&
            episode is PodcastEpisode &&
            hlsStreamUrl != null &&
            !episode.downloadUrl.isNullOrBlank() &&
            // A downloaded episode always plays its local file, so the stream toggle can't do anything.
            !episode.isDownloaded
        return uiState.value.copy(
            shelfItems = shelfItems.filter { it.showIf(episode) && (it != ShelfItem.StreamSelector || canStreamToggle) },
            episode = episode,
            isTranscriptAvailable = isTranscriptAvailable,
            alternateEnclosures = alternateEnclosures,
            hlsStreamUrl = hlsStreamUrl,
            playerSource = episode.playerSource(selectedStreams, hlsStreamUrl),
        )
    }

    /**
     * Reflects the stream that is actually playing. With HLS streaming on, the HLS stream is the
     * default before the user picks anything, so fall back to it when there is no explicit choice.
     */
    private fun BaseEpisode?.playerSource(selectedStreams: Map<String, SelectedStream>, hlsStreamUrl: String?): PlayerSource {
        val episode = this as? PodcastEpisode ?: return PlayerSource.Primary
        hlsStreamUrl ?: return PlayerSource.Primary
        val defaultUri = if (FeatureFlag.isEnabled(Feature.HLS_STREAMING)) hlsStreamUrl else episode.streamUrl
        val effectiveUri = selectedStreams[episode.uuid]?.uri ?: defaultUri
        return if (effectiveUri == hlsStreamUrl) PlayerSource.Alternative else PlayerSource.Primary
    }

    fun onEffectsClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Effects, source)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowEffectsOption)
        }
    }

    fun onStreamToggleClick(source: ShelfItemSource) {
        // No analytics yet (no dedicated ShelfActionType). source kept for a uniform shelf-action signature.
        val episode = uiState.value.episode as? PodcastEpisode ?: return
        // More than one alternate stream is a choice, not a toggle, so open the selector dialog instead.
        if (uiState.value.playableAlternateStreamCount > 1) {
            viewModelScope.launch { _navigationState.emit(NavigationState.ShowStreamSelector) }
            return
        }
        val hlsUrl = uiState.value.hlsStreamUrl ?: return
        val downloadUrl = episode.downloadUrl?.takeIf { it.isNotBlank() } ?: return
        val stream = if (uiState.value.playerSource == PlayerSource.Alternative) {
            SelectedStream(uri = downloadUrl, contentType = episode.fileType)
        } else {
            SelectedStream(uri = hlsUrl, contentType = MimeTypes.APPLICATION_M3U8)
        }
        playbackManager.selectStream(episode.uuid, stream)
    }

    fun onSleepClick(source: ShelfItemSource) {
        trackShelfAction(ShelfItem.Sleep, source)
        viewModelScope.launch {
            val hasChapters = !playbackManager.playbackStateFlow.firstOrNull()?.chapters.isNullOrEmpty()
            _navigationState.emit(NavigationState.ShowSleepTimerOptions(hasChapters))
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
                    ) {
                        launch { episodeManager.disableAutoDownload(episode) }
                        onArchiveConfirmed(episode)
                    },
                )
            } else if (episode is UserEpisode) {
                val deleteState = CloudDeleteHelper.getDeleteState(episode)
                val deleteFunction: (UserEpisode, DeleteState) -> Unit = { episode, deleteState ->
                    launch { episodeManager.disableAutoDownload(episode) }
                    CloudDeleteHelper.deleteEpisode(
                        episode = episode,
                        deleteState = deleteState,
                        sourceView = SourceView.PLAYER,
                        downloadQueue = downloadQueue,
                        playbackManager = playbackManager,
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
        podcastUuid: String,
        source: ShelfItemSource,
    ) {
        trackShelfAction(ShelfItem.AddToPlaylist, source)
        viewModelScope.launch {
            _navigationState.emit(
                NavigationState.AddEpisodeToPlaylist(
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
        }
    }

    fun onMoreClick() {
        eventHorizon.track(PlayerShelfOverflowMenuShownEvent)
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowMoreActions)
        }
    }

    fun trackShelfAction(
        shelfItem: ShelfItem,
        shelfItemSource: ShelfItemSource,
    ) {
        eventHorizon.track(
            PlayerShelfActionTappedEvent(
                from = shelfItemSource.analyticsValue,
                action = shelfItem.analyticsValue,
            ),
        )
        if (shelfItem == ShelfItem.Cast) {
            chromeCastAnalytics.trackChromeCastViewShown()
        }
    }

    data class UiState(
        val shelfItems: List<ShelfItem> = emptyList(),
        val episode: BaseEpisode? = null,
        val isTranscriptAvailable: Boolean = false,
        val alternateEnclosures: List<EpisodeAlternateEnclosure> = emptyList(),
        val hlsStreamUrl: String? = null,
        val playerSource: PlayerSource = PlayerSource.Primary,
    ) {
        val playerShelfItems: List<ShelfItem>
            get() = shelfItems.take(MIN_SHELF_ITEMS_SIZE)
        val playerBottomSheetShelfItems: List<ShelfItem>
            get() = shelfItems.drop(MIN_SHELF_ITEMS_SIZE)

        /** Alternate enclosures that expose a streamable http(s) source. */
        val playableAlternateStreamCount: Int
            get() = alternateEnclosures.count { enclosure -> enclosure.sources.any { it.uri.startsWith("http", ignoreCase = true) } }
    }

    /** The source the player is currently playing: [Primary] is the progressive file, [Alternative] is the HLS stream. */
    enum class PlayerSource { Primary, Alternative }

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
        data class ShowSleepTimerOptions(val hasChapters: Boolean) : NavigationState
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
        data object ShowStreamSelector : NavigationState
        data object ShowAddBookmark : NavigationState
        data class StartUpsellFlow(val source: OnboardingUpgradeSource) : NavigationState
        data class AddEpisodeToPlaylist(val episodeUuid: String, val podcastUuid: String) : NavigationState
    }

    sealed interface SnackbarMessage {
        data object EpisodeDownloadStarted : SnackbarMessage
        data object EpisodeRemoved : SnackbarMessage
        data object TranscriptNotAvailable : SnackbarMessage
        data object ShareNotAvailable : SnackbarMessage
    }

    enum class ShelfItemSource(
        val analyticsValue: ShelfActionSourceType,
    ) {
        Shelf(
            analyticsValue = ShelfActionSourceType.Shelf,
        ),
        OverflowMenu(
            analyticsValue = ShelfActionSourceType.OverflowMenu,
        ),
    }

    companion object {
        const val MIN_SHELF_ITEMS_SIZE = 4
    }
}
