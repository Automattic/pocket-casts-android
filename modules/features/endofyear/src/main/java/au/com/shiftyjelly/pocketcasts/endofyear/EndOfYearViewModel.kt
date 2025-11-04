package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity.StoriesSource
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearStats
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManager
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.utils.coroutines.CachedAction
import au.com.shiftyjelly.pocketcasts.utils.extensions.padEnd
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
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
    @Assisted private val topListTitle: String,
    @Assisted private val source: StoriesSource,
    private val endOfYearSync: EndOfYearSync,
    private val endOfYearManager: EndOfYearManager,
    settings: Settings,
    private val listServiceManager: ListServiceManager,
    private val sharingClient: StorySharingClient,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val syncState = MutableStateFlow<SyncState>(SyncState.Syncing)

    private val eoyStatsAction = CachedAction<Year, Pair<EndOfYearStats, RandomShowIds?>> {
        val stats = endOfYearManager.getStats(year)
        topPodcastsLinkAction.run(stats.topPodcasts, scope = viewModelScope)
        stats to getRandomShowIds(stats)
    }

    private val topPodcastsLink = MutableStateFlow<String?>(null)
    private val topPodcastsLinkAction = CachedAction<List<TopPodcast>, Unit> { topPodcasts ->
        if (topPodcasts.isNotEmpty()) {
            val podcasts = topPodcasts.map { Podcast(uuid = it.uuid) }
            runCatching {
                val link = listServiceManager.createPodcastList(
                    title = topListTitle,
                    description = "",
                    podcasts = podcasts,
                )
                topPodcastsLink.emit(link)
            }
        }
    }

    private val progress = MutableStateFlow(0f)
    private var countDownJob: Job? = null
    private val storyAutoProgressPauseReasons = MutableStateFlow(setOf(StoryProgressPauseReason.ScreenInBackground))

    private val _switchStory = MutableSharedFlow<Unit>()
    internal val switchStory get() = _switchStory.asSharedFlow()

    internal val uiState = combine(
        syncState,
        settings.cachedSubscription.flow,
        topPodcastsLink,
        progress,
        ::createUiModel,
    ).stateIn(viewModelScope, SharingStarted.Lazily, UiState.Syncing)

    internal fun syncData() {
        viewModelScope.launch {
            syncState.emit(SyncState.Syncing)
            val isSynced = endOfYearSync.sync(year)
            if (!isSynced) {
                trackFailedToLoad()
            }
            syncState.emit(if (isSynced) SyncState.Synced else SyncState.Failure)
        }
    }

    private suspend fun createUiModel(
        syncState: SyncState,
        subscription: Subscription?,
        topPodcastsLink: String?,
        progress: Float,
    ) = when (syncState) {
        SyncState.Syncing -> UiState.Syncing
        SyncState.Failure -> UiState.Failure
        SyncState.Synced -> {
            val (stats, randomShowIds) = eoyStatsAction.run(year, viewModelScope).await()
            val stories = createStories(stats, randomShowIds, subscription, topPodcastsLink)
            UiState.Synced(
                stories = stories,
                isPaidAccount = subscription != null,
                storyProgress = progress,
            )
        }
    }

    private fun createStories(
        stats: EndOfYearStats,
        randomShowIds: RandomShowIds?,
        subscription: Subscription?,
        topPodcastsLink: String?,
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
            add(Story.TopShows(stats.topPodcasts, topPodcastsLink))
        }
        add(Story.Ratings(stats.ratingStats))
        add(Story.TotalTime(stats.playbackTime))
        val longestEpisode = stats.longestEpisode
        if (longestEpisode != null) {
            add(Story.LongestEpisode(longestEpisode))
        }
        if (subscription == null) {
            add(Story.PlusInterstitial)
        }
        add(
            Story.YearVsYear(
                lastYearDuration = stats.lastYearPlaybackTime,
                thisYearDuration = stats.playbackTime,
                subscriptionTier = subscription?.tier,
            ),
        )
        add(
            Story.CompletionRate(
                listenedCount = stats.playedEpisodeCount,
                completedCount = stats.completedEpisodeCount,
                subscriptionTier = subscription?.tier,
            ),
        )
        add(Story.Ending)
    }

    internal fun onStoryChanged(story: Story) {
        trackStoryShown(story)
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
            } else {
                progress.value = 1f
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

    internal fun share(story: Story, screenshot: File) {
        val request = SharingRequest
            .endOfYearStory(story, year, screenshot)
            .build()
        viewModelScope.launch { sharingClient.shareStory(request) }
    }

    internal fun trackFailedToLoad() {
        trackEvent(AnalyticsEvent.END_OF_YEAR_STORIES_FAILED_TO_LOAD)
    }

    internal fun trackStoriesShown() {
        trackEvent(
            AnalyticsEvent.END_OF_YEAR_STORIES_SHOWN,
            mapOf("source" to source.value),
        )
    }

    internal fun trackStoriesClosed(source: String) {
        trackEvent(
            AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED,
            mapOf("source" to source),
        )
    }

    internal fun trackStoriesAutoFinished() {
        trackEvent(
            AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED,
            mapOf("source" to "auto_progress"),
        )
    }

    internal fun trackStoryShown(story: Story) {
        trackEvent(
            AnalyticsEvent.END_OF_YEAR_STORY_SHOWN,
            mapOf("story" to story.analyticsValue),
        )
    }

    internal fun trackReplayStoriesTapped() {
        trackEvent(AnalyticsEvent.END_OF_YEAR_STORY_REPLAY_BUTTON_TAPPED)
    }

    internal fun trackUpsellShown() {
        trackEvent(AnalyticsEvent.END_OF_YEAR_UPSELL_SHOWN)
    }

    internal fun trackLearnRatingsShown() {
        trackEvent(AnalyticsEvent.END_OF_YEAR_LEARN_RATINGS_SHOWN)
    }

    private fun trackEvent(
        event: AnalyticsEvent,
        properties: Map<String, Any> = emptyMap(),
    ) {
        analyticsTracker.track(
            event,
            buildMap {
                putAll(properties)
                put("year", year.value)
            },
        )
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
        fun create(
            year: Year,
            topListTitle: String,
            source: StoriesSource,
        ): EndOfYearViewModel
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
    ScreenshotDialog,
    TakingScreenshot,
}
