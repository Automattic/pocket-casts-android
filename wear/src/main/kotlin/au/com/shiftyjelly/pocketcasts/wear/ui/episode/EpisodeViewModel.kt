package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import android.app.Application
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.profile.cloud.AddFileActivity
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextPosition
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine6
import au.com.shiftyjelly.pocketcasts.wear.di.ForApplicationScope
import au.com.shiftyjelly.pocketcasts.wear.ui.player.AudioOutputSelectorHelper
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpisodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadManager: DownloadManager,
    private val episodeAnalytics: EpisodeAnalytics,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val showNotesManager: ServerShowNotesManager,
    theme: Theme,
    @ApplicationContext appContext: Context,
    @ForApplicationScope private val coroutineScope: CoroutineScope,
    private val audioOutputSelectorHelper: AudioOutputSelectorHelper,
) : AndroidViewModel(appContext as Application) {
    private var playAttempt: Job? = null
    private val sourceView = SourceView.EPISODE_DETAILS

    sealed class State {
        data class Loaded(
            val episode: BaseEpisode,
            val podcast: Podcast?,
            val isPlayingEpisode: Boolean,
            val inUpNext: Boolean,
            val tintColor: Color?,
            val downloadProgress: Float? = null,
            val showNotesState: ShowNotesState,
            val errorData: ErrorData?,
        ) : State() {
            data class ErrorData(
                @StringRes val errorTitleRes: Int,
                @DrawableRes val errorIconRes: Int,
                val errorDescription: String?,
            )
        }

        object Empty : State()
    }

    data class UpNextOption(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        val onClick: () -> Unit,
    )

    val upNextOptions = listOf(
        UpNextOption(
            iconRes = IR.drawable.ic_upnext_playnext,
            titleRes = LR.string.play_next,
            onClick = { addToUpNext(UpNextPosition.NEXT) },
        ),
        UpNextOption(
            iconRes = IR.drawable.ic_upnext_playlast,
            titleRes = LR.string.play_last,
            onClick = { addToUpNext(UpNextPosition.LAST) },
        ),
    )

    val stateFlow: StateFlow<State>

    // SharedFlow used for one shot operation like navigating to the Now Playing screen
    private val _showNowPlaying = MutableSharedFlow<Boolean>()
    val showNowPlaying = _showNowPlaying.asSharedFlow()

    init {
        val episodeUuid = savedStateHandle.get<String>(EpisodeScreenFlow.episodeUuidArgument)
            ?: throw IllegalStateException("EpisodeViewModel must have an episode uuid in the SavedStateHandle")

        val episodeFlow = episodeManager.observeEpisodeByUuid(episodeUuid)

        val podcastFlow = episodeFlow
            .filterIsInstance<PodcastEpisode>()
            .map { podcastManager.findPodcastByUuidSuspend(it.podcastUuid) }

        val isPlayingEpisodeFlow = playbackManager.playbackStateRelay.asFlow()
            .filter { it.episodeUuid == episodeUuid }
            .map { it.isPlaying }

        val inUpNextFlow = playbackManager.upNextQueue.changesObservable.asFlow()

        val downloadProgressFlow = combine(
            episodeFlow,
            downloadManager.progressUpdateRelay.asFlow()
        ) { episode, downloadProgressUpdate ->
            (episode to downloadProgressUpdate)
        }.filter { (episode, downloadProgressUpdate) ->
            episode.uuid == downloadProgressUpdate.episodeUuid
        }.map { (_, downloadProgressUpdate) ->
            downloadProgressUpdate.downloadProgress
        }

        val showNotesFlow = episodeFlow
            .flatMapLatest {
                when (it) {
                    is PodcastEpisode -> showNotesManager.loadShowNotesFlow(
                        podcastUuid = it.podcastUuid,
                        episodeUuid = it.uuid,
                    )

                    // user episodes don't have show notes
                    is UserEpisode -> flowOf(ShowNotesState.NotFound)
                }
            }

        stateFlow = combine6(
            episodeFlow,
            // Emitting a value "onStart" for the flows that shouldn't block the UI
            podcastFlow.onStart { emit(null) },
            isPlayingEpisodeFlow.onStart { emit(false) },
            inUpNextFlow,
            downloadProgressFlow.onStart<Float?> { emit(null) },
            showNotesFlow
        ) { episode, podcast, isPlayingEpisode, upNext, downloadProgress, showNotesState ->

            State.Loaded(
                episode = episode,
                podcast = podcast,
                isPlayingEpisode = isPlayingEpisode,
                downloadProgress = downloadProgress,
                inUpNext = isInUpNext(upNext, episode),
                tintColor = getTintColor(episode, podcast, theme),
                showNotesState = showNotesState,
                errorData = getErrorData(episode),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), State.Empty)
    }

    private fun getErrorData(episode: BaseEpisode): State.Loaded.ErrorData? {
        val errorTitleRes: Int?
        val errorIconRes: Int?
        var errorDescription: String? = null

        val episodeStatus = episode.episodeStatus
        if (episode.playErrorDetails == null) {
            errorTitleRes = when (episodeStatus) {
                EpisodeStatusEnum.DOWNLOAD_FAILED -> LR.string.podcasts_download_failed
                EpisodeStatusEnum.WAITING_FOR_WIFI -> LR.string.podcasts_download_wifi
                EpisodeStatusEnum.WAITING_FOR_POWER -> LR.string.podcasts_download_power
                else -> null
            }
            if (episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                errorDescription = episode.downloadErrorDetails
            }
            errorIconRes = when (episodeStatus) {
                EpisodeStatusEnum.DOWNLOAD_FAILED -> IR.drawable.ic_failedwarning
                EpisodeStatusEnum.WAITING_FOR_WIFI -> IR.drawable.ic_waitingforwifi
                EpisodeStatusEnum.WAITING_FOR_POWER -> IR.drawable.ic_waitingforpower
                else -> null
            }
        } else {
            errorIconRes = IR.drawable.ic_play_all
            errorTitleRes = LR.string.podcast_episode_playback_error
            errorDescription = episode.playErrorDetails
        }
        return errorTitleRes?.let {
            State.Loaded.ErrorData(
                errorTitleRes = it,
                errorIconRes = errorIconRes ?: IR.drawable.ic_failedwarning,
                errorDescription = errorDescription
            )
        }
    }

    private fun isInUpNext(
        upNext: UpNextQueue.State?,
        episode: BaseEpisode,
    ) =
        (upNext is UpNextQueue.State.Loaded) &&
            (upNext.queue + upNext.episode)
                .map { it.uuid }
                .contains(episode.uuid)

    private suspend fun getTintColor(
        episode: BaseEpisode,
        podcast: Podcast?,
        theme: Theme,
    ): Color? = when (episode) {
        is PodcastEpisode ->
            podcast?.getTintColor(theme.isDarkTheme)?.let { podcastTint ->
                val tint = ThemeColor.podcastIcon02(theme.activeTheme, podcastTint)
                Color(tint)
            }
        is UserEpisode ->
            // First check if the user has set a custom color for this episode
            AddFileActivity.darkThemeColors().find {
                episode.tintColorIndex == it.tintColorIndex
            }?.let {
                Color(it.color)
            } ?: extractColorFromEpisodeArtwork(episode)
    }

    fun downloadEpisode() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val fromString = "wear episode screen"
            clearErrors(episode)
            if (episode.downloadTaskId != null) {
                when (episode) {
                    is PodcastEpisode -> {
                        episodeManager.stopDownloadAndCleanUp(episode, fromString)
                    }
                    is UserEpisode -> {
                        downloadManager.removeEpisodeFromQueue(episode, fromString)
                    }
                }

                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_CANCELLED,
                    source = sourceView,
                    uuid = episode.uuid
                )
            } else if (!episode.isDownloaded) {
                episode.autoDownloadStatus =
                    PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                downloadManager.addEpisodeToQueue(episode, fromString, true)

                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED,
                    source = sourceView,
                    uuid = episode.uuid
                )
            }
        }
    }

    private suspend fun clearErrors(episode: BaseEpisode) {
        withContext(Dispatchers.IO) {
            if (episode is PodcastEpisode) {
                episodeManager.clearDownloadError(episode)
            }
            episodeManager.clearPlaybackError(episode)
        }
    }

    fun deleteDownloadedEpisode() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            episodeManager.deleteEpisodeFile(
                episode,
                playbackManager,
                disableAutoDownload = true,
                removeFromUpNext = true
            )
            episodeAnalytics.trackEvent(
                event = AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                source = sourceView,
                uuid = episode.uuid,
            )
        }
    }

    fun onPlayClicked(showStreamingConfirmation: () -> Unit) {
        if (playbackManager.shouldWarnAboutPlayback()) {
            showStreamingConfirmation()
        } else {
            playAttempt?.cancel()

            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    fun onStreamingConfirmationResult(result: StreamingConfirmationScreen.Result) {
        val confirmedStreaming = result == StreamingConfirmationScreen.Result.CONFIRMED
        if (confirmedStreaming && !playbackManager.isPlaying()) {
            playAttempt?.cancel()

            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    private fun play() {
        val episode = (stateFlow.value as? State.Loaded)?.episode
            ?: return
        viewModelScope.launch {
            if (episode.playErrorDetails != null || episode.downloadErrorDetails != null) {
                clearErrors(episode)
            }
            playbackManager.playNowSync(
                episode = episode,
                sourceView = sourceView,
            )
            _showNowPlaying.emit(true)
        }
    }

    fun onPauseClicked() {
        if ((stateFlow.value as? State.Loaded)?.isPlayingEpisode != true) {
            Timber.e("Attempted to pause when not playing")
            return
        }
        playAttempt?.cancel()

        viewModelScope.launch {
            playbackManager.pause(sourceView = sourceView)
        }
    }

    fun addToUpNext(upNextPosition: UpNextPosition) {
        val state = stateFlow.value as? State.Loaded ?: return
        viewModelScope.launch {
            playbackManager.play(
                upNextPosition = upNextPosition,
                episode = state.episode,
                source = sourceView
            )
        }
    }

    private fun removeFromUpNext() {
        val state = stateFlow.value as? State.Loaded ?: return
        playbackManager.removeEpisode(
            episodeToRemove = state.episode,
            source = SourceView.EPISODE_DETAILS
        )
    }

    fun onUpNextClicked(
        onRemoveFromUpNext: () -> Unit,
        navigateToUpNextOptions: () -> Unit
    ) {
        val state = stateFlow.value as? State.Loaded ?: return

        val wasInUpNext = state.inUpNext

        if (wasInUpNext) {
            removeFromUpNext()
            onRemoveFromUpNext()
        } else if (playbackManager.upNextQueue.queueEpisodes.isNotEmpty()) {
            navigateToUpNextOptions()
        } else {
            // If the Up Next queue is empty, it doesn't matter where we add the episode
            addToUpNext(UpNextPosition.NEXT)
        }
    }

    fun onArchiveClicked() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        if (episode !is PodcastEpisode) {
            Timber.e("Attempted to archive a non-podcast episode")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (episode.isArchived) {
                episodeManager.unarchive(episode)
                episodeAnalytics.trackEvent(
                    AnalyticsEvent.EPISODE_UNARCHIVED,
                    sourceView,
                    episode.uuid
                )
            } else {
                episodeManager.archive(episode, playbackManager)
                episodeAnalytics.trackEvent(
                    AnalyticsEvent.EPISODE_ARCHIVED,
                    sourceView,
                    episode.uuid
                )
            }
        }
    }

    fun onStarClicked() {
        (stateFlow.value as? State.Loaded)?.episode?.let { episode ->
            if (episode !is PodcastEpisode) {
                Timber.e("Attempted to star a non-podcast episode")
                return
            }

            viewModelScope.launch {
                episodeManager.toggleStarEpisode(episode, sourceView)
            }
        }
    }

    fun onMarkAsPlayedClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            (stateFlow.value as? State.Loaded)?.episode?.let { episode ->
                val event = if (episode.playingStatus == EpisodePlayingStatus.COMPLETED) {
                    episodeManager.markAsNotPlayed(episode)
                    AnalyticsEvent.EPISODE_MARKED_AS_UNPLAYED
                } else {
                    episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
                    AnalyticsEvent.EPISODE_MARKED_AS_PLAYED
                }
                episodeAnalytics.trackEvent(event, sourceView, episode.uuid)
            }
        }
    }

    private suspend fun extractColorFromEpisodeArtwork(userEpisode: UserEpisode): Color? =
        userEpisode.artworkUrl?.let { artworkUrl ->
            val context = getApplication<Application>()
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false) // Disable hardware bitmaps.
                .build()

            val result = (loader.execute(request) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap

            // Set a timeout to make sure the user isn't blocked for too long just
            // because we're trying to extract a tint color.
            withTimeoutOrNull(2000L) {
                suspendCoroutine { continuation ->
                    Palette.from(bitmap).generate { palette ->
                        val lightVibrantHsl = palette?.lightVibrantSwatch?.hsl
                        continuation.resume(
                            lightVibrantHsl?.let { hsl ->
                                Color.hsl(hsl[0], hsl[1], hsl[2])
                            }
                        )
                    }
                }
            }
        }
}
