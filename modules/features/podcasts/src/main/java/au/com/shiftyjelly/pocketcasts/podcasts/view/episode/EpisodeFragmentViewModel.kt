package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.content.Context
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressCache
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsUnplayedEvent
import com.automattic.eventhorizon.EpisodeUnarchivedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class EpisodeFragmentViewModel @Inject constructor(
    val episodeManager: EpisodeManager,
    val podcastManager: PodcastManager,
    val theme: Theme,
    val playbackManager: PlaybackManager,
    val settings: Settings,
    private val downloadQueue: DownloadQueue,
    private val downloadProgressCache: DownloadProgressCache,
    private val showNotesManager: ShowNotesManager,
    private val eventHorizon: EventHorizon,
    private val transcriptManager: TranscriptManager,
    private val userManager: UserManager,
) : ViewModel(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val source = SourceView.EPISODE_DETAILS
    lateinit var state: LiveData<EpisodeFragmentState>
    lateinit var showNotesState: LiveData<ShowNotesState>
    val isPlaying: LiveData<Boolean> = playbackManager.playbackStateLive.map {
        it.episodeUuid == episode?.uuid && it.isPlaying
    }

    var episode: PodcastEpisode? = null
    var podcast: Podcast? = null
    var isFragmentChangingConfigurations: Boolean = false

    private var startPlaybackTimestamp: Duration? = null
    private var autoDispatchPlay = false

    private var loadTranscriptJob: Job? = null

    data class EpisodePageState(
        val transcript: Transcript? = null,
        val isPlusUser: Boolean = false,
    )

    private val _pageState = MutableStateFlow(EpisodePageState())
    val pageState = _pageState.asStateFlow()

    init {
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _pageState.update { state ->
                    state.copy(isPlusUser = signInState.isSignedInAsPlusOrPatron)
                }
            }
        }
    }

    fun setup(
        episodeUuid: String,
        podcastUuid: String?,
        timestamp: Duration?,
        autoPlay: Boolean,
        forceDark: Boolean,
    ) {
        startPlaybackTimestamp = timestamp
        autoDispatchPlay = autoPlay
        val isDarkTheme = forceDark || theme.isDarkTheme
        val downloadProgressFlow = downloadProgressCache
            .progressFlow(episodeUuid)
            .map { progress -> (progress?.percentage?.toFloat() ?: 0f) / 100 }
            .distinctUntilChanged()

        // Without both an episode and a podcast there is no state to emit, so the screen stays blank
        val stateFlow = flow<EpisodeFragmentState> {
            val episode = findEpisode(episodeUuid, podcastUuid) ?: return@flow
            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return@flow
            val tintColor = podcast.getTintColor(isDarkTheme)
            emitAll(
                combine(
                    episodeManager.findByUuidFlow(episodeUuid),
                    showNotesManager.loadShowNotesFlow(podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid),
                    downloadProgressFlow,
                ) { episodeLoaded, showNotesState, downloadProgress ->
                    EpisodeFragmentState.Loaded(
                        episode = episodeLoaded,
                        podcast = podcast,
                        showNotesState = showNotesState,
                        tintColor = tintColor,
                        podcastColor = tintColor,
                        downloadProgress = downloadProgress,
                    )
                },
            )
        }
            .onEach(::onStateLoaded)
            .catch { error -> emit(EpisodeFragmentState.Error(error)) }
            .flowOn(Dispatchers.IO)

        // asLiveData won't re-run a completed flow, so keep it open to retry the load on the next onActive
        // No inactive grace period, so a dismissed screen cannot still emit and trigger auto play
        state = flow {
            emitAll(stateFlow)
            awaitCancellation()
        }.asLiveData(timeoutInMs = 0)

        showNotesState = state
            .map { episodeState ->
                when (episodeState) {
                    is EpisodeFragmentState.Loaded -> episodeState.showNotesState
                    is EpisodeFragmentState.Error -> ShowNotesState.NotFound
                }
            }
            .distinctUntilChanged()

        if (pageState.value.transcript?.episodeUuid != episodeUuid) {
            val oldJob = loadTranscriptJob
            loadTranscriptJob = launch {
                oldJob?.cancelAndJoin()
                val transcript = transcriptManager.loadTranscript(episodeUuid)
                _pageState.update { state ->
                    state.copy(transcript = transcript)
                }
            }
        }
    }

    private suspend fun findEpisode(episodeUuid: String, podcastUuid: String?): PodcastEpisode? {
        val episode = episodeManager.findByUuid(episodeUuid)
        if (episode != null || podcastUuid == null) {
            return episode
        }
        // Not in the database, so try to load the episode from the server
        val podcast = podcastManager.findOrDownloadPodcast(podcastUuid)
        return podcast.episodes.find { it.uuid == episodeUuid }
            ?: episodeManager.downloadMissingEpisode(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                skeletonEpisode = PodcastEpisode(uuid = episodeUuid, publishedDate = Date()),
                downloadMetaData = true,
            ) as? PodcastEpisode
    }

    private fun onStateLoaded(episodeState: EpisodeFragmentState) {
        if (episodeState !is EpisodeFragmentState.Loaded) {
            return
        }
        if (autoDispatchPlay) {
            val playTimestamp = startPlaybackTimestamp
            autoDispatchPlay = false
            startPlaybackTimestamp = null
            play(episodeState.episode, playTimestamp)
        }
        episode = episodeState.episode
        podcast = episodeState.podcast
    }

    fun deleteDownloadedEpisode() {
        episode?.let { episode ->
            downloadQueue.cancel(episode.uuid, source)
            launch {
                episodeManager.disableAutoDownload(episode)
            }
        }
    }

    fun downloadEpisode() {
        val episode = episode ?: return
        if (episode.isDownloadCancellable) {
            downloadQueue.cancel(episode.uuid, source)
        } else if (!episode.isDownloaded) {
            downloadQueue.enqueue(episode.uuid, DownloadType.UserTriggered(waitForWifi = false), source)
        }
        launch {
            episodeManager.clearPlaybackErrorBlocking(episode)
        }
    }

    fun markAsPlayedClicked(isOn: Boolean) {
        launch {
            episode?.let { episode ->
                val event = if (isOn) {
                    episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
                    EpisodeMarkedAsPlayedEvent(
                        episodeUuid = episode.uuid,
                        source = source.analyticsValue,
                    )
                } else {
                    episodeManager.markAsNotPlayedBlocking(episode)
                    EpisodeMarkedAsUnplayedEvent(
                        episodeUuid = episode.uuid,
                        source = source.analyticsValue,
                    )
                }
                eventHorizon.track(event)
            }
        }
    }

    fun addToUpNextTop() {
        episode?.let { episode ->
            launch { playbackManager.playNext(episode = episode, source = source) }
        }
    }

    fun addToUpNextBottom() {
        episode?.let { episode ->
            launch { playbackManager.playLast(episode = episode, source = source) }
        }
    }

    fun removeFromUpNext() {
        episode?.let { episode ->
            launch { playbackManager.removeEpisode(episodeToRemove = episode, source = source) }
        }
    }

    fun isEpisodeInUpNext(): Boolean {
        return playbackManager.upNextQueue.allEpisodes.any { it.uuid == episode?.uuid }
    }

    fun isUpNextEmpty(): Boolean {
        return playbackManager.upNextQueue.queueEpisodes.isEmpty()
    }

    fun seekToTimeMs(positionMs: Int) {
        playbackManager.seekToTimeMs(positionMs)
    }

    fun isCurrentlyPlayingEpisode(): Boolean {
        return playbackManager.getCurrentEpisode()?.uuid == episode?.uuid
    }

    fun archiveClicked(isOn: Boolean) {
        launch {
            episode?.let { episode ->
                val event = if (isOn) {
                    episodeManager.archiveBlocking(episode, playbackManager)
                    EpisodeArchivedEvent(
                        episodeUuid = episode.uuid,
                        source = source.analyticsValue,
                    )
                } else {
                    episodeManager.unarchiveBlocking(episode)
                    EpisodeUnarchivedEvent(
                        episodeUuid = episode.uuid,
                        source = source.analyticsValue,
                    )
                }
                eventHorizon.track(event)
            }
        }
    }

    fun shouldShowStreamingWarning(context: Context): Boolean {
        return isPlaying.value == false && episode?.isDownloaded == false && settings.warnOnMeteredNetwork.value && !Network.isUnmeteredConnection(context)
    }

    fun playClickedGetShouldClose(
        warningsHelper: WarningsHelper,
        showedStreamWarning: Boolean,
        force: Boolean = false,
        fromListUuid: String? = null,
    ): Boolean {
        episode?.let { episode ->
            val timestamp = startPlaybackTimestamp
            when {
                isPlaying.value == true -> {
                    playbackManager.pause(sourceView = source)
                    return false
                }

                timestamp != null -> {
                    startPlaybackTimestamp = null
                    autoDispatchPlay = false
                    play(episode, timestamp)
                    return true
                }

                else -> {
                    startPlaybackTimestamp = null
                    autoDispatchPlay = false
                    fromListUuid?.let { listId ->
                        eventHorizon.track(
                            DiscoverListEpisodePlayEvent(
                                listId = listId,
                                podcastUuid = episode.podcastUuid,
                            ),
                        )
                    }
                    playbackManager.playNow(
                        episode = episode,
                        forceStream = force,
                        showedStreamWarning = showedStreamWarning,
                        sourceView = source,
                    )
                    warningsHelper.showBatteryWarningSnackbarIfAppropriate()
                    return true
                }
            }
        }

        return false
    }

    private fun play(
        episode: BaseEpisode,
        timestamp: Duration?,
    ) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            playbackManager.playNowSync(episode, sourceView = source)
            if (timestamp != null) {
                playbackManager.seekToTimeMsSuspend(timestamp.toInt(DurationUnit.MILLISECONDS))
            }
        }
    }

    fun starClicked() {
        episode?.let { episode ->
            viewModelScope.launch {
                episodeManager.toggleStarEpisode(episode, source)
            }
        }
    }
}

sealed class EpisodeFragmentState {
    data class Loaded(
        val episode: PodcastEpisode,
        val podcast: Podcast,
        val showNotesState: ShowNotesState,
        @ColorInt val tintColor: Int,
        @ColorInt val podcastColor: Int,
        val downloadProgress: Float,
    ) : EpisodeFragmentState()

    data class Error(val error: Throwable) : EpisodeFragmentState()
}
