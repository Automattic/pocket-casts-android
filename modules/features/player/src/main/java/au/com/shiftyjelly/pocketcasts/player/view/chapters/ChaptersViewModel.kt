package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.toChapterOriginType
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.GeneratedChapterSeek
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.GeneratedChapterSeeker
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.GeneratedChaptersAvailability
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.ChapterLinkClickedEvent
import com.automattic.eventhorizon.ChapterOriginType
import com.automattic.eventhorizon.ChaptersShownEvent
import com.automattic.eventhorizon.ChaptersShownSource
import com.automattic.eventhorizon.DeselectChaptersChapterDeselectedEvent
import com.automattic.eventhorizon.DeselectChaptersChapterSelectedEvent
import com.automattic.eventhorizon.DeselectChaptersToggledOffEvent
import com.automattic.eventhorizon.DeselectChaptersToggledOnEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.GeneratedChaptersToggleSourceType
import com.automattic.eventhorizon.PlayerChapterSelectedEvent
import com.automattic.eventhorizon.SettingsGeneralGeneratedChaptersToggledEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel(assistedFactory = ChaptersViewModel.Factory::class)
class ChaptersViewModel @AssistedInject constructor(
    @Assisted private val mode: Mode,
    private val chapterManager: ChapterManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val generatedChapterSeeker: GeneratedChapterSeeker,
    private val transcriptManager: TranscriptManager,
    private val generatedChaptersAvailability: GeneratedChaptersAvailability,
    private val timeSource: TimeSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isTogglingChapters = MutableStateFlow(false)

    init {
        viewModelScope.launch(ioDispatcher) {
            combine(
                mode.episodeIdFlow().filter { it.isNotEmpty() },
                generatedChaptersAvailability.isAllowedFlow,
                ::Pair,
            ).collectLatest { (episodeId, isAllowed) ->
                if (!isAllowed) return@collectLatest
                var retryDelay = 5.seconds
                while (!transcriptManager.loadGeneratedChapters(episodeId) && retryDelay <= 2.minutes) {
                    delay(retryDelay)
                    retryDelay *= 2
                }
            }
        }
    }

    val uiState = mode.uiStateFlow().stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    val resolvingChapterIndex = generatedChapterSeeker
        .resolvingChapterIndex(playbackManager.playbackStateFlow.map { it.episodeUuid })
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Mode.uiStateFlow() = when (this) {
        is Mode.Episode -> createUiStateFlow(episodeId)
        is Mode.Player -> episodeIdFlow().flatMapLatest(::createUiStateFlow)
    }

    private fun Mode.episodeIdFlow() = when (this) {
        is Mode.Episode -> flowOf(episodeId)

        is Mode.Player ->
            playbackManager.playbackStateFlow
                .map { it.episodeUuid }
                .distinctUntilChanged()
    }

    private fun createUiStateFlow(episodeId: String) = combine(
        playbackManager.playbackStateFlow,
        episodeManager.findEpisodeByUuidFlow(episodeId),
        chapterManager.observerChaptersForEpisode(episodeId),
        settings.cachedSubscription.flow,
        isTogglingChapters,
        ::createUiState,
    )

    private var playChapterJob: Job? = null

    private val _showPlayer = MutableSharedFlow<Unit>()
    val showPlayer = _showPlayer.asSharedFlow()

    private val _chapterStartNotFound = Channel<Unit>(Channel.CONFLATED)
    val chapterStartNotFound = _chapterStartNotFound.receiveAsFlow()

    fun playChapter(chapter: Chapter) {
        val tappedAt = timeSource.markNow()
        val selection = ChapterSelection(chapter.origin.toChapterOriginType(), mode.chaptersShownSource)
        playChapterJob?.cancel()
        playChapterJob = viewModelScope.launch(ioDispatcher) {
            val playbackState = playbackManager.playbackStateFlow.first()
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackState.episodeUuid
            }
            val episode = episodeManager.findEpisodeByUuid(episodeId)
            selection.episode = episode

            when {
                playbackState.episodeUuid == episodeId && playbackState.positionMs.milliseconds in chapter -> _showPlayer.emit(Unit)

                playbackState.episodeUuid == episodeId -> {
                    val pendingSeek = playbackManager.skipToChapter(chapter)
                    if (!playbackState.isPlaying) {
                        playbackManager.playNowSuspend(episodeId)
                    }
                    val seek = pendingSeek.await() ?: return@launch
                    selection.generatedSeek = seek.generated
                    if (seek.isGeneratedFallback) {
                        _chapterStartNotFound.send(Unit)
                    }
                    selection.playbackStartLatency = awaitPlaybackStart(episodeId, seek.position, tappedAt)
                        ?.minus(seek.generated?.resolveDuration ?: Duration.ZERO)
                        ?.coerceAtLeast(Duration.ZERO)
                }

                playbackState.episodeUuid != episodeId -> {
                    episode ?: return@launch
                    episode.playedUpToMs = chapter.startTime.inWholeMilliseconds.toInt()
                    episodeManager.updatePlayedUpToBlocking(episode, chapter.startTime.inWholeSeconds.toDouble(), forceUpdate = true)
                    playbackManager.playNowSuspend(episode)
                    selection.playbackStartLatency = awaitPlaybackStart(episodeId, chapter.startTime, tappedAt)
                }
            }
        }.apply {
            invokeOnCompletion { eventHorizon.track(selection.toEvent()) }
        }
    }

    private suspend fun awaitPlaybackStart(episodeId: String, position: Duration, tappedAt: TimeMark) = withTimeoutOrNull(PLAYBACK_START_TIMEOUT) {
        val window = position - PLAYBACK_START_TOLERANCE..position + PLAYBACK_START_WINDOW
        playbackManager.playbackStateFlow.first { state ->
            state.episodeUuid == episodeId && state.isPlaying && state.positionMs.milliseconds in window
        }
        tappedAt.elapsedNow()
    }

    private val Mode.chaptersShownSource get() = when (this) {
        is Mode.Episode -> ChaptersShownSource.EpisodeDetails
        is Mode.Player -> ChaptersShownSource.FullscreenPlayer
    }

    private class ChapterSelection(
        val origin: ChapterOriginType,
        val source: ChaptersShownSource,
    ) {
        var episode: BaseEpisode? = null
        var generatedSeek: GeneratedChapterSeek? = null
        var playbackStartLatency: Duration? = null

        fun toEvent() = PlayerChapterSelectedEvent(
            origin = origin,
            source = source,
            playbackStartLatencyMs = playbackStartLatency?.inWholeMilliseconds,
            episodeUuid = episode?.uuid,
            podcastUuid = episode?.podcastOrSubstituteUuid,
        )
    }

    private val _showUpsell = MutableSharedFlow<Unit>()
    val showUpsell = _showUpsell.asSharedFlow()

    fun enableTogglingOrUpsell(enable: Boolean) {
        if (uiState.value.canSkipChapters) {
            isTogglingChapters.value = enable
            trackSkipChaptersToggled(enable)
        } else {
            viewModelScope.launch { _showUpsell.emit(Unit) }
        }
    }

    fun turnOffGeneratedChapters() {
        setShowGeneratedChapters(false, GeneratedChaptersToggleSourceType.ChaptersMenu)
    }

    fun turnOnGeneratedChapters() {
        setShowGeneratedChapters(true, GeneratedChaptersToggleSourceType.ChaptersOffState)
    }

    fun undoTurnOffGeneratedChapters() {
        setShowGeneratedChapters(true, GeneratedChaptersToggleSourceType.Undo)
    }

    private fun setShowGeneratedChapters(show: Boolean, source: GeneratedChaptersToggleSourceType) {
        eventHorizon.track(SettingsGeneralGeneratedChaptersToggledEvent(enabled = show, source = source))
        settings.showGeneratedChapters.set(show, updateModifiedAt = true)
    }

    fun selectChapter(select: Boolean, chapter: Chapter) {
        viewModelScope.launch(ioDispatcher) {
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackManager.playbackStateFlow.first().episodeUuid
            }
            chapterManager.selectChapter(episodeId, chapter.index, select)
            episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                trackChapterSelectionToggled(episode, chapter, select)
            }
        }
    }

    private val _scrollToChapter = MutableSharedFlow<Chapter>()
    val scrollToChapter = _scrollToChapter.asSharedFlow()

    fun scrollToChapter(chapter: Chapter) {
        viewModelScope.launch { _scrollToChapter.emit(chapter) }
    }

    fun trackChaptersShown(source: ChaptersShownSource) {
        viewModelScope.launch(ioDispatcher) {
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackManager.playbackStateFlow.first().episodeUuid
            }

            val episode = episodeManager.findEpisodeByUuid(episodeId) ?: return@launch
            val chapters = chapterManager.observerChaptersForEpisode(episodeId).first()
            if (chapters.isEmpty()) return@launch
            eventHorizon.track(
                ChaptersShownEvent(
                    episodeUuid = episodeId,
                    podcastUuid = episode.podcastOrSubstituteUuid,
                    origin = chapters.origin.toChapterOriginType(),
                    source = source,
                ),
            )
        }
    }

    fun trackChapterLinkTap(chapter: Chapter) {
        viewModelScope.launch(ioDispatcher) {
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackManager.playbackStateFlow.first().episodeUuid
            }

            episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                eventHorizon.track(
                    ChapterLinkClickedEvent(
                        episodeUuid = episodeId,
                        podcastUuid = episode.podcastOrSubstituteUuid,
                        chapterTitle = chapter.title,
                        origin = chapter.origin.toChapterOriginType(),
                    ),
                )
            }
        }
    }

    private fun createUiState(
        playbackState: PlaybackState,
        episode: BaseEpisode,
        chapters: Chapters,
        subscription: Subscription?,
        isToggling: Boolean,
    ) = UiState(
        podcast = playbackState.podcast,
        allChapters = chapters.toChapterStates(playbackPosition(playbackState, episode)),
        hasGeneratedChapters = chapters.hasGeneratedChapters,
        isTogglingChapters = isToggling,
        canSkipChapters = subscription != null,
        showHeader = episode is PodcastEpisode,
        isGeneratedChaptersOff = chapters.hasHiddenGeneratedChapters,
        showUpsell = subscription == null &&
            chapters.isEmpty() &&
            episode is PodcastEpisode &&
            FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS),
    )

    private fun playbackPosition(playbackState: PlaybackState, episode: BaseEpisode) = when (mode) {
        is Mode.Episode -> if (playbackState.episodeUuid == mode.episodeId) {
            playbackState.positionMs
        } else {
            episode.playedUpToMs
        }

        is Mode.Player -> playbackState.positionMs
    }.milliseconds

    private fun Chapters.toChapterStates(playbackPosition: Duration): List<ChapterState> {
        return map { chapter ->
            when {
                playbackPosition in chapter -> ChapterState.Playing(chapter.calculateProgress(playbackPosition), chapter)
                playbackPosition > chapter.startTime -> ChapterState.Played(chapter)
                else -> ChapterState.NotPlayed(chapter)
            }
        }
    }

    private fun trackChapterSelectionToggled(episode: BaseEpisode, chapter: Chapter, selected: Boolean) {
        val event = if (selected) {
            DeselectChaptersChapterSelectedEvent(
                episodeUuid = episode.uuid,
                podcastUuid = episode.podcastOrSubstituteUuid,
                origin = chapter.origin.toChapterOriginType(),
            )
        } else {
            DeselectChaptersChapterDeselectedEvent(
                episodeUuid = episode.uuid,
                podcastUuid = episode.podcastOrSubstituteUuid,
                origin = chapter.origin.toChapterOriginType(),
            )
        }
        eventHorizon.track(event)
    }

    private fun trackSkipChaptersToggled(checked: Boolean) {
        val event = if (checked) {
            DeselectChaptersToggledOnEvent
        } else {
            DeselectChaptersToggledOffEvent(
                numberOfDeselectedChapters = uiState.value.deselectedChaptersCount.toLong(),
            )
        }
        eventHorizon.track(event)
    }

    data class UiState(
        val podcast: Podcast? = null,
        private val allChapters: List<ChapterState> = emptyList(),
        val hasGeneratedChapters: Boolean = false,
        val isTogglingChapters: Boolean = false,
        val canSkipChapters: Boolean = false,
        val showHeader: Boolean = false,
        val isGeneratedChaptersOff: Boolean = false,
        val showUpsell: Boolean = false,
    ) {
        val chaptersCount = allChapters.size
        val showOffState get() = chaptersCount == 0 && isGeneratedChaptersOff
        val showChaptersTab get() = chaptersCount > 0 || isGeneratedChaptersOff || showUpsell
        val deselectedChaptersCount get() = allChapters.count { !it.chapter.selected }
        val chapters get() = if (isTogglingChapters) allChapters else allChapters.filter { it.chapter.selected }
        val showSubscriptionIcon get() = !isTogglingChapters && !canSkipChapters
    }

    sealed interface ChapterState {
        val chapter: Chapter

        data class Played(override val chapter: Chapter) : ChapterState
        data class Playing(val progress: Float, override val chapter: Chapter) : ChapterState
        data class NotPlayed(override val chapter: Chapter) : ChapterState
    }

    sealed interface Mode {
        data class Episode(val episodeId: String) : Mode
        data object Player : Mode
    }

    private companion object {
        val PLAYBACK_START_TIMEOUT = 30.seconds
        val PLAYBACK_START_TOLERANCE = 1.seconds
        val PLAYBACK_START_WINDOW = 10.seconds
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: Mode): ChaptersViewModel
    }
}
