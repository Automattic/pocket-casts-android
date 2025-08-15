package au.com.shiftyjelly.pocketcasts.endofyear

import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.Story.CompletionRate
import au.com.shiftyjelly.pocketcasts.models.to.Story.Cover
import au.com.shiftyjelly.pocketcasts.models.to.Story.Ending
import au.com.shiftyjelly.pocketcasts.models.to.Story.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Story.NumberOfShows
import au.com.shiftyjelly.pocketcasts.models.to.Story.PlusInterstitial
import au.com.shiftyjelly.pocketcasts.models.to.Story.Ratings
import au.com.shiftyjelly.pocketcasts.models.to.Story.TopShow
import au.com.shiftyjelly.pocketcasts.models.to.Story.TopShows
import au.com.shiftyjelly.pocketcasts.models.to.Story.TotalTime
import au.com.shiftyjelly.pocketcasts.models.to.Story.YearVsYear
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearStats
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManager
import au.com.shiftyjelly.pocketcasts.servers.list.PodcastList
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import java.time.Instant
import java.time.Year
import java.util.Date
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

@OptIn(ExperimentalCoroutinesApi::class)
class EndOfYearViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: EndOfYearViewModel

    private val endOfYearSync = FakeEndOfYearSync()
    private val endOfYearManager = FakeEofYearManager()
    private val listServiceManager = FakeListServiceManager()
    private val plusSubscription = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now(),
        isAutoRenewing = true,
        giftDays = 0,
    )
    private val patronSubscription = plusSubscription.copy(tier = SubscriptionTier.Patron)
    private val subscriptionFlow = MutableStateFlow<Subscription?>(plusSubscription)

    private val stats = EndOfYearStats(
        playedEpisodeCount = 100,
        completedEpisodeCount = 50,
        playedPodcastIds = List(13) { "id-$it" },
        playbackTime = 200.minutes,
        lastYearPlaybackTime = 20.minutes,
        topPodcasts = List(5) {
            TopPodcast(
                uuid = "top-id-$it",
                title = "top-title-$it",
                author = "author-$it",
                playbackTimeSeconds = 100.0 * it,
                playedEpisodeCount = it,
            )
        },
        longestEpisode = LongestEpisodeData(
            episodeId = "longest-id",
            episodeTitle = "longest-title",
            podcastId = "longest-podcas-id",
            podcastTitle = "longest-podcast-title",
            durationSeconds = 420.0,
            coverUrl = "longest-cover-url",
        ),
        ratingStats = RatingStats(
            ones = 0,
            twos = 35,
            threes = 66,
            fours = 17,
            fives = 89,
        ),
    )

    @Before
    fun setUp() {
        val settings = mock<Settings>()
        val userSetting = mock<UserSetting<Subscription?>>()
        whenever(userSetting.flow).thenReturn(subscriptionFlow)
        whenever(settings.cachedSubscription).thenReturn(userSetting)

        viewModel = EndOfYearViewModel(
            year = Year.of(1000),
            topListTitle = "Top list title",
            source = StoriesActivity.StoriesSource.UNKNOWN,
            endOfYearSync = endOfYearSync,
            endOfYearManager = endOfYearManager,
            settings = settings,
            listServiceManager = listServiceManager,
            sharingClient = FakeSharingClient(),
            analyticsTracker = AnalyticsTracker.test(),
        )
    }

    @Test
    fun `initialize in syncing state`() = runTest {
        viewModel.uiState.test {
            assertEquals(UiState.Syncing, awaitItem())
        }
    }

    @Test
    fun `failure when data not synced`() = runTest {
        endOfYearSync.isSynced.add(false)

        viewModel.syncData()
        viewModel.uiState.test {
            assertEquals(UiState.Failure, awaitItem())
        }
    }

    @Test
    fun `success when data is synced`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            assertTrue(awaitItem() is UiState.Synced)
        }
    }

    @Test
    fun `stories are in correct order`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertEquals(11, stories.size)
            assertTrue(stories[0] is Cover)
            assertTrue(stories[1] is NumberOfShows)
            assertTrue(stories[2] is TopShow)
            assertTrue(stories[3] is TopShows)
            assertTrue(stories[4] is Ratings)
            assertTrue(stories[5] is TotalTime)
            assertTrue(stories[6] is LongestEpisode)
            assertTrue(stories[7] is PlusInterstitial)
            assertTrue(stories[8] is YearVsYear)
            assertTrue(stories[9] is CompletionRate)
            assertTrue(stories[10] is Ending)
        }
    }

    @Test
    fun `number of shows`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<NumberOfShows>()

            assertEquals(stats.playedPodcastCount, story.showCount)
            assertEquals(stats.playedEpisodeCount, story.epsiodeCount)
            // We select 8 random episodes to display in the UI
            assertEquals(4, story.topShowIds.distinct().size)
            assertEquals(4, story.bottomShowIds.distinct().size)
            assertEquals(emptySet<String>(), story.topShowIds.intersect(story.bottomShowIds))
            assertTrue(stats.playedPodcastIds.containsAll(story.topShowIds))
        }
    }

    @Test
    fun `number of shows for a single show`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(playedPodcastIds = listOf("id")))

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<NumberOfShows>()

            assertEquals(List(4) { "id" }, story.topShowIds)
            assertEquals(List(4) { "id" }, story.bottomShowIds)
        }
    }

    @Test
    fun `number of shows for three shows`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(playedPodcastIds = List(3) { "id-$it" }))

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<NumberOfShows>()

            assertEquals(4, story.topShowIds.size)
            assertEquals(4, story.bottomShowIds.size)

            // Verify list rotation
            assertEquals(story.topShowIds[0], story.topShowIds[3])
            assertEquals(story.topShowIds[1], story.bottomShowIds[0])
            assertEquals(story.topShowIds[2], story.bottomShowIds[1])
            assertEquals(story.topShowIds[3], story.bottomShowIds[2])
            assertEquals(story.bottomShowIds[0], story.bottomShowIds[3])
        }
    }

    @Test
    fun `number of shows for six shows`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(playedPodcastIds = List(6) { "id-$it" }))

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<NumberOfShows>()

            assertEquals(4, story.topShowIds.size)
            assertEquals(4, story.bottomShowIds.size)

            // Verify list rotation
            assertEquals(story.topShowIds[0], story.bottomShowIds[2])
            assertEquals(story.topShowIds[1], story.bottomShowIds[3])
        }
    }

    @Test
    fun `number of shows for no podcast ids`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(playedPodcastIds = emptyList()))

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<NumberOfShows>(stories)
        }
    }

    @Test
    fun `top show`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<TopShow>()

            assertEquals(
                TopShow(stats.topPodcasts.first()),
                story,
            )
        }
    }

    @Test
    fun `top show for no top podcasts`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(topPodcasts = emptyList()))

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<TopShow>(stories)
        }
    }

    @Test
    fun `top shows`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<TopShows>()

            assertEquals(
                TopShows(stats.topPodcasts, podcastListUrl = null),
                story,
            )
        }
    }

    @Test
    fun `top shows with podcast list link`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            assertEquals(
                TopShows(stats.topPodcasts, podcastListUrl = null),
                awaitStory<TopShows>(),
            )

            listServiceManager.podcastListUrl.complete("podcast-list-url")
            assertEquals(
                TopShows(stats.topPodcasts, podcastListUrl = "podcast-list-url"),
                awaitStory<TopShows>(),
            )
        }
    }

    @Test
    fun `top shows for no top podcasts`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(topPodcasts = emptyList()))

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<TopShows>(stories)
        }
    }

    @Test
    fun `ratings`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<Ratings>()

            assertEquals(
                Ratings(stats.ratingStats),
                story,
            )
        }
    }

    @Test
    fun `total time`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<TotalTime>()

            assertEquals(
                TotalTime(stats.playbackTime),
                story,
            )
        }
    }

    @Test
    fun `longest episode`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<LongestEpisode>()

            assertEquals(
                LongestEpisode(stats.longestEpisode!!),
                story,
            )
        }
    }

    @Test
    fun `longest episode for no episode`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(longestEpisode = null))

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<LongestEpisode>(stories)
        }
    }

    @Test
    fun `plus interstitial for regular user`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertHasStory<PlusInterstitial>(stories)
        }
    }

    @Test
    fun `plus interstitial for Plus user`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<PlusInterstitial>(stories)
        }
    }

    @Test
    fun `plus interstitial for Patron user`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = patronSubscription

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            assertDoesNotHaveStory<PlusInterstitial>(stories)
        }
    }

    @Test
    fun `year vs year`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<YearVsYear>()

            assertEquals(
                YearVsYear(
                    lastYearDuration = stats.lastYearPlaybackTime,
                    thisYearDuration = stats.playbackTime,
                    subscriptionTier = SubscriptionTier.Plus,
                ),
                story,
            )
        }
    }

    @Test
    fun `completion rate`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<CompletionRate>()

            assertEquals(
                CompletionRate(
                    listenedCount = stats.playedEpisodeCount,
                    completedCount = stats.completedEpisodeCount,
                    subscriptionTier = SubscriptionTier.Plus,
                ),
                story,
            )
        }
    }

    @Test
    fun `update stories with subscription tier`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        viewModel.uiState.test {
            assertHasStory<PlusInterstitial>(awaitStories())

            subscriptionFlow.value = plusSubscription
            endOfYearManager.stats.add(stats)
            assertDoesNotHaveStory<PlusInterstitial>(awaitStories())

            subscriptionFlow.value = null
            endOfYearManager.stats.add(stats)
            assertHasStory<PlusInterstitial>(awaitStories())
        }
    }

    @Test
    fun `auto switch stories`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        viewModel.switchStory.test {
            expectNoEvents()

            viewModel.onStoryChanged(stories.getStoryOfType<Cover>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<NumberOfShows>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<TopShow>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<TopShows>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<Ratings>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<TotalTime>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<LongestEpisode>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<PlusInterstitial>())
            expectNoEvents()

            viewModel.onStoryChanged(stories.getStoryOfType<YearVsYear>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<CompletionRate>())
            assertEquals(Unit, awaitItem())

            viewModel.onStoryChanged(stories.getStoryOfType<Ending>())
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `resume and pause stories auto switching`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        viewModel.switchStory.test {
            expectNoEvents()

            // Initially switching should be paused
            viewModel.onStoryChanged(stories.getStoryOfType<Cover>())
            expectNoEvents()

            // Resume after pause
            viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
            assertEquals(Unit, awaitItem())

            // Pause after resume
            viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
            viewModel.onStoryChanged(stories.getStoryOfType<NumberOfShows>())
            expectNoEvents()
        }
    }

    @Test
    fun `pause story auto progress as long as there is at least one reason`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        viewModel.switchStory.test {
            expectNoEvents()

            // Initially switching should be paused
            viewModel.onStoryChanged(stories.getStoryOfType<Cover>())
            expectNoEvents()

            viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.UserHoldingStory)
            viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
            expectNoEvents()

            viewModel.pauseStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
            expectNoEvents()

            viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.UserHoldingStory)
            viewModel.resumeStoryAutoProgress(StoryProgressPauseReason.ScreenInBackground)
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `get index of next story for paid account`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        assertEquals(1, viewModel.getNextStoryIndex(stories.indexOf<Cover>()))
        assertEquals(2, viewModel.getNextStoryIndex(stories.indexOf<NumberOfShows>()))
        assertEquals(3, viewModel.getNextStoryIndex(stories.indexOf<TopShow>()))
        assertEquals(4, viewModel.getNextStoryIndex(stories.indexOf<TopShows>()))
        assertEquals(5, viewModel.getNextStoryIndex(stories.indexOf<Ratings>()))
        assertEquals(6, viewModel.getNextStoryIndex(stories.indexOf<TotalTime>()))
        assertEquals(7, viewModel.getNextStoryIndex(stories.indexOf<LongestEpisode>()))
        assertEquals(8, viewModel.getNextStoryIndex(stories.indexOf<YearVsYear>()))
        assertEquals(9, viewModel.getNextStoryIndex(stories.indexOf<CompletionRate>()))
        assertEquals(null, viewModel.getNextStoryIndex(stories.indexOf<Ending>()))
    }

    @Test
    fun `get index of previous story for paid account`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        assertEquals(null, viewModel.getPreviousStoryIndex(stories.indexOf<Cover>()))
        assertEquals(0, viewModel.getPreviousStoryIndex(stories.indexOf<NumberOfShows>()))
        assertEquals(1, viewModel.getPreviousStoryIndex(stories.indexOf<TopShow>()))
        assertEquals(2, viewModel.getPreviousStoryIndex(stories.indexOf<TopShows>()))
        assertEquals(3, viewModel.getPreviousStoryIndex(stories.indexOf<Ratings>()))
        assertEquals(4, viewModel.getPreviousStoryIndex(stories.indexOf<TotalTime>()))
        assertEquals(5, viewModel.getPreviousStoryIndex(stories.indexOf<LongestEpisode>()))
        assertEquals(6, viewModel.getPreviousStoryIndex(stories.indexOf<YearVsYear>()))
        assertEquals(7, viewModel.getPreviousStoryIndex(stories.indexOf<CompletionRate>()))
        assertEquals(8, viewModel.getPreviousStoryIndex(stories.indexOf<Ending>()))
    }

    @Test
    fun `get index of next story for free account`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        assertEquals(1, viewModel.getNextStoryIndex(stories.indexOf<Cover>()))
        assertEquals(2, viewModel.getNextStoryIndex(stories.indexOf<NumberOfShows>()))
        assertEquals(3, viewModel.getNextStoryIndex(stories.indexOf<TopShow>()))
        assertEquals(4, viewModel.getNextStoryIndex(stories.indexOf<TopShows>()))
        assertEquals(5, viewModel.getNextStoryIndex(stories.indexOf<Ratings>()))
        assertEquals(6, viewModel.getNextStoryIndex(stories.indexOf<TotalTime>()))
        assertEquals(7, viewModel.getNextStoryIndex(stories.indexOf<LongestEpisode>()))
        assertEquals(10, viewModel.getNextStoryIndex(stories.indexOf<PlusInterstitial>()))
        assertEquals(10, viewModel.getNextStoryIndex(stories.indexOf<YearVsYear>()))
        assertEquals(10, viewModel.getNextStoryIndex(stories.indexOf<CompletionRate>()))
        assertEquals(null, viewModel.getNextStoryIndex(stories.indexOf<Ending>()))
    }

    @Test
    fun `get index of previous story for free account`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        val stories = (viewModel.uiState.first() as UiState.Synced).stories

        assertEquals(null, viewModel.getPreviousStoryIndex(stories.indexOf<Cover>()))
        assertEquals(0, viewModel.getPreviousStoryIndex(stories.indexOf<NumberOfShows>()))
        assertEquals(1, viewModel.getPreviousStoryIndex(stories.indexOf<TopShow>()))
        assertEquals(2, viewModel.getPreviousStoryIndex(stories.indexOf<TopShows>()))
        assertEquals(3, viewModel.getPreviousStoryIndex(stories.indexOf<Ratings>()))
        assertEquals(4, viewModel.getPreviousStoryIndex(stories.indexOf<TotalTime>()))
        assertEquals(5, viewModel.getPreviousStoryIndex(stories.indexOf<LongestEpisode>()))
        assertEquals(6, viewModel.getPreviousStoryIndex(stories.indexOf<PlusInterstitial>()))
        assertEquals(7, viewModel.getPreviousStoryIndex(stories.indexOf<YearVsYear>()))
        assertEquals(7, viewModel.getPreviousStoryIndex(stories.indexOf<CompletionRate>()))
        assertEquals(7, viewModel.getPreviousStoryIndex(stories.indexOf<Ending>()))
    }

    @Test
    fun `plus interstitial has max progress`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionFlow.value = null

        viewModel.syncData()
        viewModel.uiState.test {
            val stories = awaitStories()

            viewModel.onStoryChanged(stories.getStoryOfType<PlusInterstitial>())
            assertEquals(1f, awaitItem().storyProgress)
        }
    }

    private suspend fun TurbineTestContext<UiState>.awaitStories(): List<Story> {
        return (awaitItem() as UiState.Synced).stories
    }

    private suspend inline fun <reified T : Story> TurbineTestContext<UiState>.awaitStory(): T {
        return awaitStories().getStoryOfType<T>()
    }

    private inline fun <reified T : Story> List<Story>.getStoryOfType(): T {
        return filterIsInstance<T>().single()
    }

    private inline fun <reified T : Story> List<Story>.indexOf(): Int {
        return indexOfFirst { it is T }
    }

    private inline fun <reified T : Story> assertHasStory(stories: List<Story>) {
        assertTrue(stories.filterIsInstance<T>().isNotEmpty())
    }

    private inline fun <reified T : Story> assertDoesNotHaveStory(stories: List<Story>) {
        assertTrue(stories.filterIsInstance<T>().isEmpty())
    }

    private class FakeEofYearManager : EndOfYearManager {
        val stats = Turbine<EndOfYearStats>()

        override suspend fun getStats(year: Year): EndOfYearStats {
            return stats.awaitItem()
        }

        override suspend fun isEligibleForEndOfYear(year: Year) = true

        override suspend fun getPlayedEpisodeCount(year: Year) = -1
    }

    private class FakeEndOfYearSync : EndOfYearSync {
        val isSynced = Turbine<Boolean>()

        override suspend fun sync(year: Year): Boolean {
            return isSynced.awaitItem()
        }

        override suspend fun reset() = Unit
    }

    private class FakeListServiceManager : ListServiceManager {
        val podcastListUrl = CompletableDeferred<String>()

        override suspend fun createPodcastList(
            title: String,
            description: String,
            podcasts: List<Podcast>,
            date: Date,
            serverSecret: String,
        ) = podcastListUrl.await()

        override suspend fun openPodcastList(listId: String) = PodcastList(
            title = "",
            description = "",
            podcasts = emptyList(),
            date = null,
            hash = null,
        )

        override fun extractShareListIdFromWebUrl(webUrl: String) = ""
    }

    private class FakeSharingClient : StorySharingClient {
        override suspend fun shareStory(request: SharingRequest): SharingResponse {
            return SharingResponse(
                isSuccessful = true,
                feedbackMessage = null,
                error = null,
            )
        }
    }
}
