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
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
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
import com.automattic.eventhorizon.PlayerChapterSelectedEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val fingerprintTimingManager: FingerprintTimingManager,
    private val appPlatform: AppPlatform,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isTogglingChapters = MutableStateFlow(false)

    val uiState = mode.uiStateFlow().stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Mode.uiStateFlow() = when (this) {
        is Mode.Episode -> createUiStateFlow(episodeId)

        is Mode.Player ->
            playbackManager.playbackStateFlow
                .map { it.episodeUuid }
                .distinctUntilChanged()
                .flatMapLatest(::createUiStateFlow)
    }

    private fun createUiStateFlow(episodeId: String) = combine(
        playbackManager.playbackStateFlow,
        episodeManager.findEpisodeByUuidFlow(episodeId),
        chapterManager.observerChaptersForEpisode(episodeId),
        settings.cachedSubscription.flow,
        isTogglingChapters,
        ::createUiState,
    ).combine(aligningFlow(episodeId)) { uiState, isAligning ->
        uiState.copy(isAligningChapters = isAligning && uiState.hasGeneratedChapters)
    }

    private fun aligningFlow(episodeId: String): Flow<Boolean> = if (appPlatform != AppPlatform.Phone || !FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)) {
        flowOf(false)
    } else {
        fingerprintTimingManager.stateFlow
            .map { state ->
                state is FingerprintTimingManager.State.Preparing &&
                    fingerprintTimingManager.activeEpisodeUuid == episodeId
            }
            .distinctUntilChanged()
    }

    private var playChapterJob: Job? = null

    private val _showPlayer = MutableSharedFlow<Unit>()
    val showPlayer = _showPlayer.asSharedFlow()

    fun playChapter(chapter: Chapter) {
        val tapMark = TimeSource.Monotonic.markNow()
        playChapterJob?.cancel()
        playChapterJob = viewModelScope.launch(ioDispatcher) {
            val stateAtTap = playbackManager.playbackStateFlow.first()
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> stateAtTap.episodeUuid
            }
            val episode = episodeManager.findEpisodeByUuid(episodeId)
            val tappedCurrentEpisode = stateAtTap.episodeUuid == episodeId

            val alignsFingerprint = tappedCurrentEpisode && chapter.isGenerated
            val alignMark = TimeSource.Monotonic.markNow()
            val target = if (tappedCurrentEpisode) {
                withTimeoutOrNull(CHAPTER_ALIGNMENT_TIMEOUT) {
                    chapterManager.awaitStreamAlignedChapter(episodeId, chapter)
                } ?: chapter
            } else {
                chapter
            }
            val alignmentWaitMs = if (alignsFingerprint) alignMark.elapsedNow().inWholeMilliseconds else 0L
            val fingerprintCalculationTimeMs = if (alignsFingerprint && fingerprintTimingManager.activeEpisodeUuid == episodeId) {
                fingerprintTimingManager.preparationDurationMs
            } else {
                null
            }

            val playbackState = playbackManager.playbackStateFlow.first()
            val alreadyInChapter = playbackState.episodeUuid == episodeId && playbackState.positionMs.milliseconds in target

            val latencyMs = if (alreadyInChapter) {
                _showPlayer.emit(Unit)
                if (playbackState.isPlaying) 0L else null
            } else {
                val playbackResumed = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(PLAYBACK_START_TIMEOUT) {
                        playbackManager.playbackStateFlow.first { it.hasResumedPlayback(episodeId, target) }
                    }
                }
                if (playbackState.episodeUuid == episodeId) {
                    playbackManager.skipToChapter(target)
                    if (!playbackState.isPlaying) {
                        playbackManager.playNowSuspend(episodeId)
                    }
                } else if (!tappedCurrentEpisode) {
                    // Only switch episodes when the tap targeted a different episode to begin with. A seek within
                    // the tapped episode must never yank playback back if the user has since moved to another one.
                    episode?.let {
                        it.playedUpToMs = target.startTime.inWholeMilliseconds.toInt()
                        episodeManager.updatePlayedUpToBlocking(it, target.startTime.inWholeSeconds.toDouble(), forceUpdate = true)
                        playbackManager.playNowSuspend(it)
                    }
                }
                if (playbackResumed.await() != null) {
                    (tapMark.elapsedNow().inWholeMilliseconds - alignmentWaitMs).coerceAtLeast(0L)
                } else {
                    null
                }
            }

            eventHorizon.track(
                PlayerChapterSelectedEvent(
                    origin = chapter.origin.toChapterOriginType(),
                    source = when (mode) {
                        is Mode.Episode -> ChaptersShownSource.EpisodeDetails
                        is Mode.Player -> ChaptersShownSource.FullscreenPlayer
                    },
                    playbackStartLatencyMs = latencyMs,
                    fingerprintCalculationTimeMs = fingerprintCalculationTimeMs,
                    episodeUuid = episodeId,
                    podcastUuid = episode?.podcastOrSubstituteUuid,
                ),
            )
        }
    }

    private fun PlaybackState.hasResumedPlayback(episodeId: String, chapter: Chapter) = episodeUuid == episodeId &&
        isPlaying &&
        positionMs.milliseconds in chapter &&
        lastChangeFrom in PLAYBACK_RESUMED_CHANGES

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
        val isAligningChapters: Boolean = false,
    ) {
        val chaptersCount = allChapters.size
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

    @AssistedFactory
    interface Factory {
        fun create(mode: Mode): ChaptersViewModel
    }

    companion object {
        private val PLAYBACK_START_TIMEOUT = 5.seconds
        private val CHAPTER_ALIGNMENT_TIMEOUT = 10.seconds
        private val PLAYBACK_RESUMED_CHANGES = setOf(
            PlaybackManager.LastChangeFrom.OnPlayerPlaying.value,
            PlaybackManager.LastChangeFrom.OnSeekComplete.value,
        )
    }
}
