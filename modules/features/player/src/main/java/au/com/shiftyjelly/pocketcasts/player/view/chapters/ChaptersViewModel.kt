package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ChaptersViewModel.Factory::class)
class ChaptersViewModel @AssistedInject constructor(
    @Assisted private val mode: Mode,
    private val chapterManager: ChapterManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
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
        episodeManager.observeEpisodeByUuid(episodeId),
        chapterManager.observerChaptersForEpisode(episodeId),
        settings.cachedSubscriptionStatus.flow,
        isTogglingChapters,
        ::createUiState,
    )

    private var playChapterJob: Job? = null

    private val _showPlayer = MutableSharedFlow<Unit>()
    val showPlayer = _showPlayer.asSharedFlow()

    fun playChapter(chapter: Chapter) {
        tracker.track(AnalyticsEvent.PLAYER_CHAPTER_SELECTED)
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
                    episodeManager.updatePlayedUpTo(episode, chapter.startTime.inWholeSeconds.toDouble(), forceUpdate = true)
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

    fun selectChapter(select: Boolean, chapter: Chapter) {
        viewModelScope.launch(ioDispatcher) {
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackManager.playbackStateFlow.first().episodeUuid
            }
            chapterManager.selectChapter(episodeId, chapter.index, select)
            episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                trackChapterSelectionToggled(episode, select)
            }
        }
    }

    private val _scrollToChapter = MutableSharedFlow<Chapter>()
    val scrollToChapter = _scrollToChapter.asSharedFlow()

    fun scrollToChapter(chapter: Chapter) {
        viewModelScope.launch { _scrollToChapter.emit(chapter) }
    }

    fun trackChapterLinkTap(chapter: Chapter) {
        viewModelScope.launch(ioDispatcher) {
            val episodeId = when (mode) {
                is Mode.Episode -> mode.episodeId
                is Mode.Player -> playbackManager.playbackStateFlow.first().episodeUuid
            }

            episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                tracker.track(
                    AnalyticsEvent.CHAPTER_LINK_CLICKED,
                    mapOf(
                        "chapter_title" to chapter.title,
                        "episode_uuid" to episodeId,
                        "podcast_uuid" to episode.podcastOrSubstituteUuid,
                    ),
                )
            }
        }
    }

    private fun createUiState(
        playbackState: PlaybackState,
        episode: BaseEpisode,
        chapters: Chapters,
        subscriptionStatus: SubscriptionStatus?,
        isToggling: Boolean,
    ) = UiState(
        podcast = playbackState.podcast,
        allChapters = chapters.toChapterStates(playbackPosition(playbackState, episode)),
        isTogglingChapters = isToggling,
        canSkipChapters = subscriptionStatus.canSkipChapters(),
        showHeader = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) && episode is PodcastEpisode,
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
        return getList().map { chapter ->
            when {
                playbackPosition in chapter -> ChapterState.Playing(chapter.calculateProgress(playbackPosition), chapter)
                playbackPosition > chapter.startTime -> ChapterState.Played(chapter)
                else -> ChapterState.NotPlayed(chapter)
            }
        }
    }

    private fun SubscriptionStatus?.canSkipChapters() = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
        Feature.isUserEntitled(Feature.DESELECT_CHAPTERS, toUserTier())

    private fun SubscriptionStatus?.toUserTier() = (this as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free

    private fun trackChapterSelectionToggled(episode: BaseEpisode, selected: Boolean) {
        tracker.track(
            if (selected) {
                AnalyticsEvent.DESELECT_CHAPTERS_CHAPTER_SELECTED
            } else {
                AnalyticsEvent.DESELECT_CHAPTERS_CHAPTER_DESELECTED
            },
            Analytics.chapterSelectionToggled(episode),
        )
    }

    private fun trackSkipChaptersToggled(checked: Boolean) {
        if (checked) {
            tracker.track(AnalyticsEvent.DESELECT_CHAPTERS_TOGGLED_ON)
        } else {
            tracker.track(
                AnalyticsEvent.DESELECT_CHAPTERS_TOGGLED_OFF,
                Analytics.skipChaptersToggled(uiState.value.deselectedChaptersCount),
            )
        }
    }

    data class UiState(
        val podcast: Podcast? = null,
        private val allChapters: List<ChapterState> = emptyList(),
        val isTogglingChapters: Boolean = false,
        val canSkipChapters: Boolean = false,
        val showHeader: Boolean = false,
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

    private object Analytics {
        private const val EPISODE_UUID = "episode_uuid"
        private const val PODCAST_UUID = "podcast_uuid"
        private const val NUMBER_OF_DESELECTED_CHAPTERS = "number_of_deselected_chapters"
        private const val UNKNOWN = "unknown"

        fun chapterSelectionToggled(episode: BaseEpisode?) = mapOf(
            EPISODE_UUID to (episode?.uuid ?: UNKNOWN),
            PODCAST_UUID to (episode?.podcastOrSubstituteUuid ?: UNKNOWN),
        )

        fun skipChaptersToggled(count: Int) = mapOf(NUMBER_OF_DESELECTED_CHAPTERS to count)
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: Mode): ChaptersViewModel
    }
}
