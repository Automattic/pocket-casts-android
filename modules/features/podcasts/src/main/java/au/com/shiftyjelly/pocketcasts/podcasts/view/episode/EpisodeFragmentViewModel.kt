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
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
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
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsUnplayedEvent
import com.automattic.eventhorizon.EpisodeSummarySourceType
import com.automattic.eventhorizon.EpisodeSummaryTappedEvent
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull

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
    private val paymentClient: PaymentClient,
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
    private var loadSummaryJob: Job? = null
    private var lastSummaryEpisodeUuid: String? = null

    enum class EpisodeContentTab { DESCRIPTION, SUMMARY, BOOKMARKS, CHAPTERS, TRANSCRIPT }

    data class EpisodePageState(
        val transcript: Transcript? = null,
        val isPlusUser: Boolean = false,
        val isFreeTrialAvailable: Boolean = false,
        val summary: String? = null,
        val selectedContentTab: EpisodeContentTab = EpisodeContentTab.DESCRIPTION,
        val episodePublishedDate: Date? = null,
        val episodeDurationMs: Long? = null,
    ) {
        internal fun selectContentTab(tab: EpisodeContentTab): EpisodePageState {
            val contentTab = when (tab) {
                EpisodeContentTab.DESCRIPTION -> EpisodeContentTab.DESCRIPTION

                EpisodeContentTab.SUMMARY -> if (summary == null) {
                    EpisodeContentTab.DESCRIPTION
                } else {
                    EpisodeContentTab.SUMMARY
                }

                EpisodeContentTab.BOOKMARKS -> EpisodeContentTab.BOOKMARKS

                EpisodeContentTab.CHAPTERS -> EpisodeContentTab.CHAPTERS

                EpisodeContentTab.TRANSCRIPT -> EpisodeContentTab.TRANSCRIPT
            }
            return copy(selectedContentTab = contentTab)
        }

        internal fun withTranscript(transcript: Transcript?): EpisodePageState {
            val contentTab = if (transcript == null && selectedContentTab == EpisodeContentTab.TRANSCRIPT) {
                EpisodeContentTab.DESCRIPTION
            } else {
                selectedContentTab
            }
            return copy(
                transcript = transcript,
                selectedContentTab = contentTab,
            )
        }

        internal fun withSummary(summary: String?): EpisodePageState {
            val contentTab = if (summary == null && selectedContentTab == EpisodeContentTab.SUMMARY) {
                EpisodeContentTab.DESCRIPTION
            } else {
                selectedContentTab
            }
            return copy(
                summary = summary,
                selectedContentTab = contentTab,
            )
        }
    }

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
        viewModelScope.launch {
            val plans = paymentClient.loadSubscriptionPlans().getOrNull()
            val hasTrial = plans?.findOfferPlan(
                SubscriptionTier.Plus,
                BillingCycle.Monthly,
                SubscriptionOffer.Trial,
            ) != null
            _pageState.update { state ->
                state.copy(isFreeTrialAvailable = hasTrial)
            }
        }
    }

    fun selectContentTab(tab: EpisodeContentTab) {
        _pageState.update { state ->
            state.selectContentTab(tab)
        }
        if (tab == EpisodeContentTab.SUMMARY) {
            val episodeUuid = episode?.uuid ?: return
            val podcastUuid = podcast?.uuid ?: return
            eventHorizon.track(
                EpisodeSummaryTappedEvent(
                    source = EpisodeSummarySourceType.EpisodeDetails,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

        // A missing episode emits no state, leaving the screen on its loading placeholder
        val stateFlow = flow { emit(findEpisode(episodeUuid, podcastUuid)) }
            .filterNotNull()
            .flatMapLatest<PodcastEpisode, EpisodeFragmentState> { episode ->
                combine(
                    episodeManager.findByUuidFlow(episodeUuid),
                    podcastFlow(episode.podcastUuid),
                    showNotesManager.loadShowNotesFlow(podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid),
                    downloadProgressFlow,
                ) { episodeLoaded, podcast, showNotesState, downloadProgress ->
                    val tintColor = podcast.getTintColor(isDarkTheme)
                    EpisodeFragmentState.Loaded(
                        episode = episodeLoaded,
                        podcast = podcast,
                        showNotesState = showNotesState,
                        tintColor = tintColor,
                        podcastColor = tintColor,
                        downloadProgress = downloadProgress,
                    )
                }
            }
            .onEach(::onStateLoaded)
            .catch { error -> emit(EpisodeFragmentState.Error(error)) }
            .flowOn(Dispatchers.IO)

        // No inactive grace period, so a dismissed screen cannot still emit and trigger auto play
        state = stateFlow.asLiveData(timeoutInMs = 0)

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
                    state.withTranscript(transcript)
                }
            }
        }

        val isSummaryEnabled = FeatureFlag.isEnabled(Feature.AI_SUMMARIES)
        val isChaptersEnabled = FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS)
        if ((isSummaryEnabled || isChaptersEnabled) && lastSummaryEpisodeUuid != episodeUuid) {
            _pageState.update { state ->
                state.withSummary(null)
            }
            val oldSummaryJob = loadSummaryJob
            loadSummaryJob = launch {
                oldSummaryJob?.cancelAndJoin()
                val result = transcriptManager.loadSummaryText(episodeUuid)
                _pageState.update { state ->
                    state.withSummary(result)
                }
                if (result != null || !isSummaryEnabled) {
                    lastSummaryEpisodeUuid = episodeUuid
                }
            }
        } else if (!isSummaryEnabled) {
            _pageState.update { state ->
                state.withSummary(null)
            }
        }
    }

    private suspend fun findEpisode(episodeUuid: String, podcastUuid: String?): PodcastEpisode? {
        val episode = episodeManager.findByUuid(episodeUuid)
        if (episode != null || podcastUuid == null) {
            return episode
        }
        // Not in the database, so if we know the podcast we can try to load the episode from the server
        val podcast = podcastManager.findOrDownloadPodcastRxSingle(podcastUuid).await()
        return podcast.episodes.find { it.uuid == episodeUuid }
            ?: episodeManager.downloadMissingEpisodeRxMaybe(
                episodeUuid,
                podcastUuid,
                PodcastEpisode(uuid = episodeUuid, publishedDate = Date()),
                podcastManager,
                downloadMetaData = true,
                source = source,
            ).awaitSingleOrNull() as? PodcastEpisode
    }

    // A missing podcast emits nothing, so the combined state never emits either
    private fun podcastFlow(podcastUuid: String): Flow<Podcast> = flow {
        podcastManager.findPodcastByUuid(podcastUuid)?.let { podcast -> emit(podcast) }
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
        _pageState.update { pageState ->
            val episodePublishedDate = episodeState.episode.publishedDate
            val episodeDurationMs = episodeState.episode.durationMs.toLong()
            if (
                pageState.episodePublishedDate == episodePublishedDate &&
                pageState.episodeDurationMs == episodeDurationMs
            ) {
                pageState
            } else {
                pageState.copy(
                    episodePublishedDate = episodePublishedDate,
                    episodeDurationMs = episodeDurationMs,
                )
            }
        }
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
