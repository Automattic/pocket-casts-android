package au.com.shiftyjelly.pocketcasts.settings.stats

import android.app.Application
import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.views.review.InAppReviewHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var statsManager: StatsManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var syncManager: SyncManager

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var inAppReviewHelper: InAppReviewHelper

    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        val mockResources = mock<Resources>()
        whenever(mockResources.getString(anyInt())).thenReturn("")
        whenever(application.resources).thenReturn(mockResources)
        whenever(syncManager.isLoggedIn()).thenReturn(true)
    }

    @Test
    fun `given last 7 days played upto sum more than 2_5 hrs, when stats are loaded, then app review dialog is shown`() =
        runTest {
            initViewModel(playedUpToSumInHours = 3.0)

            viewModel.loadStats()

            assertTrue((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    @Test
    fun `given last 7 days played upto sum less than 2_5 hrs, when stats are loaded, then app review dialog  is not shown`() =
        runTest {
            initViewModel(playedUpToSumInHours = 2.0)

            viewModel.loadStats()

            assertFalse((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    @Test
    fun `given stats started more than 7 days, when stats are loaded, then app review dialog is shown`() =
        runTest {
            initViewModel(statsStartedAt = LocalDateTime.now().minusDays(8.toLong()))

            viewModel.loadStats()

            assertTrue((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    @Test
    fun `given stats started less than 7 days, when stats are loaded, then app review dialog is not shown`() =
        runTest {
            initViewModel(statsStartedAt = LocalDateTime.now().minusDays(6.toLong()))

            viewModel.loadStats()

            assertFalse((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    @Test
    fun `given stats started equal to 7 days, when stats are loaded, then app review dialog is not shown`() =
        runTest {
            initViewModel(statsStartedAt = LocalDateTime.now().minusDays(7.toLong()))

            viewModel.loadStats()

            assertFalse((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    @Test
    fun `given stats started at unix epoch, when stats are loaded, then app review dialog is not shown`() =
        runTest {
            initViewModel(statsStartedAt = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()))

            viewModel.loadStats()

            assertFalse((viewModel.state.value as StatsViewModel.State.Loaded).showAppReviewDialog)
        }

    private suspend fun initViewModel(
        statsStartedAt: LocalDateTime = LocalDateTime.now().minusDays(8.toLong()),
        playedUpToSumInHours: Double = 3.0,
    ) {
        whenever(statsManager.getServerStats()).thenReturn(
            StatsBundle(
                values = emptyMap(),
                startedAt = Date.from(statsStartedAt.atZone(ZoneId.systemDefault()).toInstant())
            )
        )

        whenever(episodeManager.calculatePlayedUptoSumInSecsWithinDays(7))
            .thenReturn(
                TimeUnit.HOURS.toSeconds(playedUpToSumInHours.toLong()).toDouble()
            )

        viewModel = StatsViewModel(
            statsManager = statsManager,
            episodeManager = episodeManager,
            settings = settings,
            syncManager = syncManager,
            application = application,
            ioDispatcher = UnconfinedTestDispatcher(),
            inAppReviewHelper = inAppReviewHelper,
        )
    }
}
