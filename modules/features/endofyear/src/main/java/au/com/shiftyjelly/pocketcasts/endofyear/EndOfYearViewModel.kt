package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

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
            UiState.Synced(stories, progress)
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
                        currentProgress += 0.01f
                        progress.value = currentProgress
                        delay(progressDelay)
                    }
                    _switchStory.emit(Unit)
                }
            }
        }
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
        override val storyProgress: Float,
    ) : UiState
}

@Immutable
internal sealed interface Story {
    val previewDuration: Duration? get() = 7.seconds

    data object Cover : Story

    @Immutable
    data class NumberOfShows(
        val showCount: Int,
        val epsiodeCount: Int,
        val topShowIds: List<String>,
        val bottomShowIds: List<String>,
    ) : Story

    data class TopShow(
        val show: TopPodcast,
    ) : Story

    @Immutable
    data class TopShows(
        val shows: List<TopPodcast>,
    ) : Story

    data class Ratings(
        val stats: RatingStats,
    ) : Story

    data class TotalTime(
        val duration: Duration,
    ) : Story

    data class LongestEpisode(
        val episode: LongestEpisodeData,
    ) : Story

    data object PlusInterstitial : Story {
        override val previewDuration: Duration? get() = null
    }

    data class YearVsYear(
        val lastYearDuration: Duration,
        val thisYearDuration: Duration,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        val yearOverYearChange
            get() = when {
                lastYearDuration == thisYearDuration -> 1.0
                lastYearDuration == Duration.ZERO -> Double.POSITIVE_INFINITY
                else -> thisYearDuration / lastYearDuration
            }
    }

    data class CompletionRate(
        val listenedCount: Int,
        val completedCount: Int,
        val subscriptionTier: SubscriptionTier?,
    ) : Story {
        val completionRate
            get() = when {
                listenedCount == 0 -> 1.0
                else -> completedCount.toDouble() / listenedCount
            }
    }

    data object Ending : Story
}

private sealed interface SyncState {
    data object Syncing : SyncState
    data object Failure : SyncState
    data object Synced : SyncState
}
