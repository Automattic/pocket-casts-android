package au.com.shiftyjelly.pocketcasts.endofyear

import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.endofyear.Story.CompletionRate
import au.com.shiftyjelly.pocketcasts.endofyear.Story.Cover
import au.com.shiftyjelly.pocketcasts.endofyear.Story.Ending
import au.com.shiftyjelly.pocketcasts.endofyear.Story.LongestEpisode
import au.com.shiftyjelly.pocketcasts.endofyear.Story.NumberOfShows
import au.com.shiftyjelly.pocketcasts.endofyear.Story.PlusInterstitial
import au.com.shiftyjelly.pocketcasts.endofyear.Story.Ratings
import au.com.shiftyjelly.pocketcasts.endofyear.Story.TopShow
import au.com.shiftyjelly.pocketcasts.endofyear.Story.TopShows
import au.com.shiftyjelly.pocketcasts.endofyear.Story.TotalTime
import au.com.shiftyjelly.pocketcasts.endofyear.Story.YearVsYear
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearStats
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.time.Year
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode as LongestEpisodeData

@OptIn(ExperimentalCoroutinesApi::class)
class EndOfYearViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: EndOfYearViewModel

    private val endOfYearSync = FakeEndOfYearSync()
    private val endOfYearManager = FakeEofYearManager()
    private val subscriptionTier = MutableStateFlow(SubscriptionTier.PLUS)

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
        viewModel = EndOfYearViewModel(
            year = Year.of(1000),
            endOfYearSync = endOfYearSync,
            endOfYearManager = endOfYearManager,
            subscriptionManager = mock { on { subscriptionTier() }.doReturn(subscriptionTier) },
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
        subscriptionTier.emit(SubscriptionTier.NONE)

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
            assertEquals(8, story.showIds.distinct().size)
            assertTrue(stats.playedPodcastIds.containsAll(story.showIds))
        }
    }

    @Test
    fun `number of shows for no podcast ids`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats.copy(playedPodcastIds = emptyList()))

        viewModel.syncData()
        viewModel.uiState.test {
            val story = awaitStory<NumberOfShows>()

            assertEquals(0, story.showIds.size)
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
                TopShows(stats.topPodcasts),
                story,
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
        subscriptionTier.emit(SubscriptionTier.NONE)

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
        subscriptionTier.emit(SubscriptionTier.PLUS)

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
        subscriptionTier.emit(SubscriptionTier.PATRON)

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
                    subscriptionTier = SubscriptionTier.PLUS,
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
                    subscriptionTier = SubscriptionTier.PLUS,
                ),
                story,
            )
        }
    }

    @Test
    fun `update stories with subscription tier`() = runTest {
        endOfYearSync.isSynced.add(true)
        endOfYearManager.stats.add(stats)
        subscriptionTier.emit(SubscriptionTier.NONE)

        viewModel.syncData()
        viewModel.uiState.test {
            assertHasStory<PlusInterstitial>(awaitStories())

            subscriptionTier.emit(SubscriptionTier.PLUS)
            endOfYearManager.stats.add(stats)
            assertDoesNotHaveStory<PlusInterstitial>(awaitStories())

            subscriptionTier.emit(SubscriptionTier.NONE)
            endOfYearManager.stats.add(stats)
            assertHasStory<PlusInterstitial>(awaitStories())
        }
    }

    private suspend fun TurbineTestContext<UiState>.awaitStories(): List<Story> {
        return (awaitItem() as UiState.Synced).stories
    }

    private suspend inline fun <reified T : Story> TurbineTestContext<UiState>.awaitStory(): T {
        return awaitStories().filterIsInstance<T>().single()
    }

    private inline fun <reified T : Story> assertHasStory(stories: List<Story>) {
        assertTrue(stories.filterIsInstance<PlusInterstitial>().isNotEmpty())
    }

    private inline fun <reified T : Story> assertDoesNotHaveStory(stories: List<Story>) {
        assertTrue(stories.filterIsInstance<PlusInterstitial>().isEmpty())
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
}
