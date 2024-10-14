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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Year
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    internal val uiState = combine(
        syncState,
        subscriptionManager.subscriptionTier(),
        ::createUiModel,
    ).stateIn(viewModelScope, SharingStarted.Lazily, UiState.Syncing)

    fun syncData() {
        viewModelScope.launch {
            syncState.emit(SyncState.Syncing)
            val isSynced = endOfYearSync.sync(year)
            syncState.emit(if (isSynced) SyncState.Synced else SyncState.Failure)
        }
    }

    private suspend fun createUiModel(
        syncState: SyncState,
        subscriptionTier: SubscriptionTier,
    ) = when (syncState) {
        SyncState.Syncing -> UiState.Syncing
        SyncState.Failure -> UiState.Failure
        SyncState.Synced -> {
            val stats = endOfYearManager.getStats(year)
            val stories = createStories(stats, subscriptionTier)
            UiState.Synced(stories)
        }
    }

    private fun createStories(
        stats: EndOfYearStats,
        subscriptionTier: SubscriptionTier,
    ): List<Story> = buildList {
        add(Story.Cover)
        add(
            Story.NumberOfShows(
                showCount = stats.playedPodcastCount,
                epsiodeCount = stats.playedEpisodeCount,
                showIds = stats.playedPodcastIds.shuffled().take(8),
            ),
        )
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

    @AssistedFactory
    interface Factory {
        fun create(year: Year): EndOfYearViewModel
    }
}

internal sealed interface UiState {
    data object Syncing : UiState

    data object Failure : UiState

    @Immutable
    data class Synced(
        val stories: List<Story>,
    ) : UiState
}

internal sealed interface Story {
    data object Cover : Story

    @Immutable
    data class NumberOfShows(
        val showCount: Int,
        val epsiodeCount: Int,
        val showIds: List<String>,
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

    data object PlusInterstitial : Story

    data class YearVsYear(
        val lastYearDuration: Duration,
        val thisYearDuration: Duration,
        val subscriptionTier: SubscriptionTier?,
    ) : Story

    data class CompletionRate(
        val listenedCount: Int,
        val completedCount: Int,
        val subscriptionTier: SubscriptionTier?,
    ) : Story

    data object Ending : Story
}

private sealed interface SyncState {
    data object Syncing : SyncState
    data object Failure : SyncState
    data object Synced : SyncState
}
