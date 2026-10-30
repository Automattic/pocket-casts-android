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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isTogglingChapters = MutableStateFlow(false)

    private val generatedChaptersLoad = MutableStateFlow<GeneratedChaptersLoad?>(null)

    init {
        viewModelScope.launch(ioDispatcher) {
            combine(
                mode.episodeIdFlow().filter { it.isNotEmpty() },
                generatedChaptersAvailability.isAllowedFlow,
                ::GeneratedChaptersLoad,
            ).collectLatest(::loadGeneratedChapters)
        }
    }

    private suspend fun loadGeneratedChapters(load: GeneratedChaptersLoad) {
        generatedChaptersLoad.value = load
        if (!load.isEnabled) return
        var isLoaded = transcriptManager.loadGeneratedChapters(load.episodeId)
        if (!isLoaded || !load.isEnabled || !awaitSavedChapters(load.episodeId)) {
            generatedChaptersLoad.value = load.copy(isFinished = true)
        }
        var retryDelay = 5.seconds
        while (!isLoaded && retryDelay <= 2.minutes) {
            delay(retryDelay)
            retryDelay *= 2
            isLoaded = transcriptManager.loadGeneratedChapters(load.episodeId)
        }
    }

    private suspend fun awaitSavedChapters(episodeId: String) = withTimeoutOrNull(SAVED_CHAPTERS_TIMEOUT) {
        chapterManager.observerChaptersForEpisode(episodeId).first { it.isNotEmpty() }
    } != null

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
        combine(settings.cachedSubscription.flow, generatedChaptersStatusFlow(episodeId), ::Pair),
        isTogglingChapters,
        ::createUiState,
    )

    private fun generatedChaptersStatusFlow(episodeId: String) = combine(
        settings.showGeneratedChapters.flow,
        generatedChaptersLoad,
    ) { isEnabled, load ->
        when {
            !isEnabled -> GeneratedChaptersStatus.NotEnabled
            load == GeneratedChaptersLoad(episodeId, isEnabled = true, isFinished = true) -> GeneratedChaptersStatus.Loaded
            else -> GeneratedChaptersStatus.Loading
        }
    }

    private var playChapterJob: Job? = null

    private val _showPlayer = MutableSharedFlow<Unit>()
    val showPlayer = _showPlayer.asSharedFlow()

    fun playChapter(chapter: Chapter) {
        eventHorizon.track(PlayerChapterSelectedEvent(origin = chapter.origin.toChapterOriginType()))
        playChapterJob?.cancel()
        playChapterJob = viewModelScope.launch(ioDispatcher) {
            val playbackState = playbackManager.playbackStateFlow.first()
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackState.episodeUuid
            }

            when {
                playbackState.episodeUuid == episodeId && playbackState.positionMs.milliseconds in chapter -> _showPlayer.emit(Unit)

                playbackState.episodeUuid == episodeId -> {
                    playbackManager.skipToChapter(chapter)
                    if (!playbackState.isPlaying) {
                        playbackManager.playNowSuspend(episodeId)
                    }
                }

                playbackState.episodeUuid != episodeId -> {
                    val episode = episodeManager.findEpisodeByUuid(episodeId) ?: return@launch
                    episode.playedUpToMs = chapter.startTime.inWholeMilliseconds.toInt()
                    episodeManager.updatePlayedUpToBlocking(episode, chapter.startTime.inWholeSeconds.toDouble(), forceUpdate = true)
                    playbackManager.playNowSuspend(episode)
                }
            }
        }
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

    fun generateChapters() {
        setShowGeneratedChapters(true, source = null)
    }

    private fun setShowGeneratedChapters(show: Boolean, source: GeneratedChaptersToggleSourceType?) {
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
        subscriptionAndStatus: Pair<Subscription?, GeneratedChaptersStatus>,
        isToggling: Boolean,
    ): UiState {
        val (subscription, generatedChaptersStatus) = subscriptionAndStatus
        val isGeneratedChaptersEpisode = episode is PodcastEpisode && FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS)
        return UiState(
            podcast = playbackState.podcast,
            allChapters = chapters.toChapterStates(playbackPosition(playbackState, episode)),
            hasGeneratedChapters = chapters.hasGeneratedChapters,
            isTogglingChapters = isToggling,
            canSkipChapters = subscription != null,
            showHeader = episode is PodcastEpisode,
            isGeneratedChaptersOff = chapters.hasHiddenGeneratedChapters,
            showUpsell = subscription == null && chapters.isEmpty() && isGeneratedChaptersEpisode,
            canGenerateChapters = subscription != null && isGeneratedChaptersEpisode,
            generatedChaptersStatus = generatedChaptersStatus,
        )
    }

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
        val canGenerateChapters: Boolean = false,
        val generatedChaptersStatus: GeneratedChaptersStatus = GeneratedChaptersStatus.NotEnabled,
    ) {
        val chaptersCount = allChapters.size
        val emptyState get() = when {
            chaptersCount > 0 -> null

            isGeneratedChaptersOff -> EmptyState.GeneratedChaptersOff

            !canGenerateChapters -> null

            else -> when (generatedChaptersStatus) {
                GeneratedChaptersStatus.NotEnabled -> EmptyState.NoChaptersYet
                GeneratedChaptersStatus.Loading -> EmptyState.GeneratingChapters
                GeneratedChaptersStatus.Loaded -> EmptyState.NoGeneratedChapters
            }
        }
        val showChaptersTab get() = chaptersCount > 0 || emptyState != null || showUpsell
        val deselectedChaptersCount get() = allChapters.count { !it.chapter.selected }
        val chapters get() = if (isTogglingChapters) allChapters else allChapters.filter { it.chapter.selected }
        val showSubscriptionIcon get() = !isTogglingChapters && !canSkipChapters
    }

    enum class GeneratedChaptersStatus {
        NotEnabled,
        Loading,
        Loaded,
    }

    enum class EmptyState {
        GeneratedChaptersOff,
        NoChaptersYet,
        GeneratingChapters,
        NoGeneratedChapters,
    }

    private data class GeneratedChaptersLoad(
        val episodeId: String,
        val isEnabled: Boolean,
        val isFinished: Boolean = false,
    )

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

    @AssistedFactory
    interface Factory {
        fun create(mode: Mode): ChaptersViewModel
    }

    private companion object {
        val SAVED_CHAPTERS_TIMEOUT = 1.seconds
    }
}
