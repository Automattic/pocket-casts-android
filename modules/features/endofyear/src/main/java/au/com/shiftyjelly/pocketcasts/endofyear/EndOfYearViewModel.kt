package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearStats
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.coroutines.CachedAction
import au.com.shiftyjelly.pocketcasts.utils.extensions.padEnd
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Year
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EndOfYearViewModel.Factory::class)
class EndOfYearViewModel @AssistedInject constructor(
    @Assisted private val year: Year,
    private val endOfYearSync: EndOfYearSync,
    private val endOfYearManager: EndOfYearManager,
    subscriptionManager: SubscriptionManager,
) : ViewModel() {
    private val syncState = MutableStateFlow<SyncState>(SyncState.Syncing)
    private val progress = MutableStateFlow(0f)
    private var countDownJob: Job? = null
    private val eoyStats = CachedAction<Year, Pair<EndOfYearStats, RandomShowIds?>> {
        val stats = endOfYearManager.getStats(year)
        stats to getRandomShowIds(stats)
    }
    private val _switchStory = MutableSharedFlow<Unit>()
    internal val switchStory get() = _switchStory.asSharedFlow()
    private val storyAutoProgressPauseReasons = MutableStateFlow(setOf(StoryProgressPauseReason.ScreenInBackground))

    internal val uiState = combine(
        syncState,
        subscriptionManager.subscriptionTier(),
        progress,
        ::createUiModel,
    ).stateIn(viewModelScope, SharingStarted.Lazily, UiState.Syncing)

    internal fun syncData() {
        viewModelScope.launch {
            syncState.emit(SyncState.Syncing)
            val isSynced = endOfYearSync.sync(year)
            syncState.emit(if (isSynced) SyncState.Synced else SyncState.Failure)
        }
    }

    private suspend fun createUiModel(
        syncState: SyncState,
        subscriptionTier: SubscriptionTier,
        progress: Float,
    ) = when (syncState) {
        SyncState.Syncing -> UiState.Syncing
        SyncState.Failure -> UiState.Failure
        SyncState.Synced -> {
            val (stats, randomShowIds) = eoyStats.run(year, viewModelScope).await()
            val stories = createStories(stats, randomShowIds, subscriptionTier)
            UiState.Synced(
                stories = stories,
                isPaidAccount = subscriptionTier.isPaid,
                storyProgress = progress,
            )
        }
    }

    private fun createStories(
        stats: EndOfYearStats,
        randomShowIds: RandomShowIds?,
        subscriptionTier: SubscriptionTier,
    ): List<Story> = buildList {
        add(Story.Cover)
        if (randomShowIds != null) {
            add(
                Story.NumberOfShows(
                    showCount = stats.playedPodcastCount,
                    epsiodeCount = stats.playedEpisodeCount,
                    topShowIds = randomShowIds.topShows,
                    bottomShowIds = randomShowIds.bottomShows,
                ),
            )
        }
        val topPodcast = stats.topPodcasts.firstOrNull()
        if (topPodcast != null) {
            add(Story.TopShow(topPodcast))
            add(Story.TopShows(stats.topPodcasts))
        }
        add(Story.Ratings(stats.ratingStats))
        add(Story.TotalTime(stats.playbackTime))
        val longestEpisode = stats.longestEpisode
        if (longestEpisode != null) {
            add(Story.LongestEpisode(longestEpisode))
        }
        if (subscriptionTier == SubscriptionTier.NONE) {
            add(Story.PlusInterstitial)
        }
        add(
            Story.YearVsYear(
                lastYearDuration = stats.lastYearPlaybackTime,
                thisYearDuration = stats.playbackTime,
                subscriptionTier = subscriptionTier,
            ),
        )
        add(
            Story.CompletionRate(
                listenedCount = stats.playedEpisodeCount,
                completedCount = stats.completedEpisodeCount,
                subscriptionTier = subscriptionTier,
            ),
        )
        add(Story.Ending)
    }

    internal fun onStoryChanged(story: Story) {
        viewModelScope.launch {
            countDownJob?.cancelAndJoin()
            progress.value = 0f
            val previewDuration = story.previewDuration
            if (previewDuration != null) {
                val progressDelay = previewDuration / 100
                countDownJob = launch {
                    var currentProgress = 0f
                    while (currentProgress < 1f) {
                        storyAutoProgressPauseReasons.first { it.isEmpty() }
                        currentProgress += 0.01f
                        progress.value = currentProgress
                        delay(progressDelay)
                    }
                    _switchStory.emit(Unit)
                }
            }
        }
    }

    internal fun resumeStoryAutoProgress(reason: StoryProgressPauseReason) {
        storyAutoProgressPauseReasons.value -= reason
    }

    internal fun pauseStoryAutoProgress(reason: StoryProgressPauseReason) {
        storyAutoProgressPauseReasons.value += reason
    }

    internal fun getNextStoryIndex(currentIndex: Int): Int? {
        val state = uiState.value as? UiState.Synced ?: return null
        val stories = state.stories

        val nextStory = stories.getOrNull(currentIndex + 1) ?: return null
        return if (state.isPaidAccount || nextStory.isFree) {
            currentIndex + 1
        } else {
            stories.drop(currentIndex + 1)
                .firstOrNull { it.isFree }
                ?.let(stories::indexOf)
        }.takeIf { it != -1 }
    }

    internal fun getPreviousStoryIndex(currentIndex: Int): Int? {
        val state = uiState.value as? UiState.Synced ?: return null
        val stories = state.stories

        val previousStory = state.stories.getOrNull(currentIndex - 1) ?: return null
        return if (state.isPaidAccount || previousStory.isFree) {
            currentIndex - 1
        } else {
            stories.take(currentIndex)
                .lastOrNull { it.isFree }
                ?.let(stories::indexOf)
        }?.takeIf { it != -1 }
    }

    private fun getRandomShowIds(stats: EndOfYearStats): RandomShowIds? {
        val showIds = stats.playedPodcastIds
        return if (showIds.isNotEmpty()) {
            val showChunks = showIds.chunked(4)
            val topShowIds = showChunks[0].padEnd(4)
            val bottomShowIds = showChunks.getOrNull(1)
                ?.plus(topShowIds)
                ?.take(4)
                .orEmpty()
                .ifEmpty { showChunks[0].padEnd(8).takeLast(4) }
            RandomShowIds(topShowIds, bottomShowIds)
        } else {
            null
        }
    }

    private data class RandomShowIds(
        val topShows: List<String>,
        val bottomShows: List<String>,
    )

    @AssistedFactory
    interface Factory {
        fun create(year: Year): EndOfYearViewModel
    }
}

@Immutable
internal sealed interface UiState {
    val storyProgress: Float get() = 0f

    data object Syncing : UiState

    data object Failure : UiState

    @Immutable
    data class Synced(
        val stories: List<Story>,
        val isPaidAccount: Boolean,
        override val storyProgress: Float,
    ) : UiState
}

private sealed interface SyncState {
    data object Syncing : SyncState
    data object Failure : SyncState
    data object Synced : SyncState
}

internal enum class StoryProgressPauseReason {
    ScreenInBackground,
    UserHoldingStory,
}
