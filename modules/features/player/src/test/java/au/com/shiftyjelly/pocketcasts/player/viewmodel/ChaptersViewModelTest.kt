package au.com.shiftyjelly.pocketcasts.player.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel.Mode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ChaptersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineRule = MainCoroutineRule(testDispatcher)

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val chapterManager = mock<ChapterManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val settings = mock<Settings>()
    private val tracker = AnalyticsTracker.test()

    private val episode = PodcastEpisode(uuid = "id", publishedDate = Date())
    private val chapters = Chapters(
        listOf(
            Chapter("1", 0.milliseconds, 100.milliseconds, selected = true, index = 0),
            Chapter("2", 101.milliseconds, 200.milliseconds, selected = true, index = 1),
            Chapter("3", 201.milliseconds, 300.milliseconds, selected = true, index = 2),
        ),
    )

    private val playbackStateFlow = MutableStateFlow(PlaybackState(episodeUuid = "id"))
    private val episodeFlow = MutableStateFlow<BaseEpisode>(episode)
    private val chaptersFlow = MutableStateFlow(chapters)
    private val subscriptionStatusFlow = MutableStateFlow<SubscriptionStatus?>(
        SubscriptionStatus.Paid(
            expiry = Date(),
            autoRenew = false,
            index = 0,
            platform = SubscriptionPlatform.GIFT,
            tier = SubscriptionTier.PATRON,
            type = SubscriptionType.PLUS,
        ),
    )

    private lateinit var chaptersViewModel: ChaptersViewModel

    @Before
    fun setup() {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        whenever(episodeManager.observeEpisodeByUuid("id")).thenReturn(episodeFlow)
        whenever(chapterManager.observerChaptersForEpisode("id")).thenReturn(chaptersFlow)
        val userSetting = mock<UserSetting<SubscriptionStatus?>>()
        whenever(userSetting.flow).thenReturn(subscriptionStatusFlow)
        whenever(settings.cachedSubscriptionStatus).thenReturn(userSetting)

        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)

        chaptersViewModel = ChaptersViewModel(
            Mode.Episode(episode.uuid),
            chapterManager,
            playbackManager,
            episodeManager,
            settings,
            tracker,
            testDispatcher,
        )
    }

    @Test
    fun `paid user can skip chapters`() = runTest {
        chaptersViewModel.uiState.test {
            assertTrue(awaitItem().canSkipChapters)
        }
    }

    @Test
    fun `paid user cant skip chapters if feature is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().canSkipChapters)
        }
    }

    @Test
    fun `free user cant skip chapters`() = runTest {
        subscriptionStatusFlow.value = SubscriptionStatus.Free()

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().canSkipChapters)
        }
    }

    @Test
    fun `unknown user cant skip chapters`() = runTest {
        subscriptionStatusFlow.value = null

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().canSkipChapters)
        }
    }

    @Test
    fun `chapters are mapped to their played statuses`() = runTest {
        playbackStateFlow.update { it.copy(positionMs = 150) }

        chaptersViewModel.uiState.test {
            val state = awaitItem()

            assertEquals(ChaptersViewModel.ChapterState.Played(chapters.getList()[0]), state.chapters[0])
            assertEquals(ChaptersViewModel.ChapterState.Playing(progress = 0.4949495f, chapters.getList()[1]), state.chapters[1])
            assertEquals(ChaptersViewModel.ChapterState.NotPlayed(chapters.getList()[2]), state.chapters[2])
        }
    }

    @Test
    fun `show header for podcast episode`() = runTest {
        chaptersViewModel.uiState.test {
            assertTrue(awaitItem().showHeader)
        }
    }

    @Test
    fun `do not show header when feature is disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().showHeader)
        }
    }

    @Test
    fun `do not show header for user episode`() = runTest {
        episodeFlow.value = UserEpisode(uuid = "id", publishedDate = Date())

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().showHeader)
        }
    }

    @Test
    fun `toggle chapter selection`() = runTest {
        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().isTogglingChapters)

            chaptersViewModel.enableTogglingOrUpsell(true)
            assertTrue(awaitItem().isTogglingChapters)

            chaptersViewModel.enableTogglingOrUpsell(false)
            assertFalse(awaitItem().isTogglingChapters)
        }
    }

    @Test
    fun `free user cant toggle chapters`() = runTest {
        subscriptionStatusFlow.value = SubscriptionStatus.Free()

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().isTogglingChapters)

            chaptersViewModel.enableTogglingOrUpsell(true)
            chaptersViewModel.enableTogglingOrUpsell(false)

            expectNoEvents()
        }
    }

    @Test
    fun `select chapter`() = runTest {
        chaptersViewModel.selectChapter(true, chapters.getList()[1])

        verifyBlocking(chapterManager, times(1)) { selectChapter("id", chapterIndex = 1, true) }
    }

    @Test
    fun `deselect chapter`() = runTest {
        chaptersViewModel.selectChapter(false, chapters.getList()[0])

        verifyBlocking(chapterManager, times(1)) { selectChapter("id", chapterIndex = 0, false) }
    }

    @Test
    fun `scroll to chapter`() = runTest {
        chaptersViewModel.scrollToChapter.test {
            expectNoEvents()

            chaptersViewModel.scrollToChapter(chapters.getList()[0])
            assertEquals(chapters.getList()[0], awaitItem())

            chaptersViewModel.scrollToChapter(chapters.getList()[0])
            assertEquals(chapters.getList()[0], awaitItem())

            chaptersViewModel.scrollToChapter(chapters.getList()[1])
            assertEquals(chapters.getList()[1], awaitItem())
        }
    }

    @Test
    fun `skip to chapter from current episode while playing`() = runTest {
        playbackStateFlow.update { it.copy(state = PlaybackState.State.PLAYING) }

        chaptersViewModel.playChapter(chapters.getList()[2])

        verify(playbackManager, times(1)).skipToChapter(chapters.getList()[2])
        verifyBlocking(playbackManager, never()) { playNowSuspend("id") }
    }

    @Test
    fun `skip to and play chapter from current episode while not playing`() = runTest {
        playbackStateFlow.update { it.copy(state = PlaybackState.State.STOPPED) }

        chaptersViewModel.playChapter(chapters.getList()[2])

        verify(playbackManager, times(1)).skipToChapter(chapters.getList()[2])
        verifyBlocking(playbackManager, times(1)) { playNowSuspend("id") }
    }

    @Test
    fun `play chapter from different episode`() = runTest {
        chaptersViewModel = ChaptersViewModel(
            Mode.Episode("id2"),
            chapterManager,
            playbackManager,
            episodeManager,
            settings,
            tracker,
            testDispatcher,
        )

        val episode = PodcastEpisode(uuid = "id2", publishedDate = Date())
        val chapter = Chapter("Chapter", startTime = 2.seconds, endTime = 3.seconds)
        whenever(episodeManager.findEpisodeByUuid("id2")).thenReturn(episode)

        chaptersViewModel.playChapter(chapter)

        verify(episodeManager, times(1)).updatePlayedUpTo(episode, chapter.startTime.inWholeSeconds.toDouble(), forceUpdate = true)
        verifyBlocking(playbackManager, times(1)) { playNowSuspend(episode) }
    }

    @Test
    fun `navigate to chapter when tapped on while playing`() = runTest {
        chaptersViewModel.showPlayer.test {
            expectNoEvents()

            chaptersViewModel.playChapter(chapters.getList()[0])
            assertEquals(Unit, awaitItem())

            chaptersViewModel.playChapter(chapters.getList()[0])
            assertEquals(Unit, awaitItem())
        }
    }
}
