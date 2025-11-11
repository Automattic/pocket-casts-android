package au.com.shiftyjelly.pocketcasts.repositories.appreview

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AppReviewManagerTest {
    private val episodesCompletedSetting = TestSetting(emptyList<Instant>())
    private val episodeStarredSetting = TestSetting<Instant?>(null)
    private val podcastRatedSetting = TestSetting<Instant?>(null)
    private val playlistCreatedSetting = TestSetting<Instant?>(null)
    private val plusUpgradedSetting = TestSetting<Instant?>(null)
    private val folderCreatedSetting = TestSetting<Instant?>(null)
    private val bookmarkCreatedSetting = TestSetting<Instant?>(null)
    private val themeChangedSetting = TestSetting<Instant?>(null)
    private val referralSharedSetting = TestSetting<Instant?>(null)
    private val playbackSharedSetting = TestSetting<Instant?>(null)

    private val submittedReasonsSetting = TestSetting(emptyList<AppReviewReason>())
    private val lastPromptSetting = TestSetting<Instant?>(null)
    private val lastDeclineTimestampsSetting = TestSetting(emptyList<Instant>())
    private val errorSessionsSetting = TestSetting(emptyList<String>())
    private val crashTimestampSetting = TestSetting<Instant?>(null)

    private val clock = MutableClock()
    private val loopIdleDuration = 1.seconds
    private var sessionIds = mutableListOf<String>()

    private val manager = AppReviewManagerImpl(
        clock = clock,
        settings = mock<Settings> {
            on { sessionIds } doReturn sessionIds
            on { appReviewEpisodeCompletedTimestamps } doReturn episodesCompletedSetting
            on { appReviewEpisodeStarredTimestamp } doReturn episodeStarredSetting
            on { appReviewPodcastRatedTimestamp } doReturn podcastRatedSetting
            on { appReviewPlaylistCreatedTimestamp } doReturn playlistCreatedSetting
            on { appReviewPlusUpgradedTimestamp } doReturn plusUpgradedSetting
            on { appReviewFolderCreatedTimestamp } doReturn folderCreatedSetting
            on { appReviewBookmarkCreatedTimestamp } doReturn bookmarkCreatedSetting
            on { appReviewThemeChangedTimestamp } doReturn themeChangedSetting
            on { appReviewReferralSharedTimestamp } doReturn referralSharedSetting
            on { appReviewPlaybackSharedTimestamp } doReturn playbackSharedSetting
            on { appReviewSubmittedReasons } doReturn submittedReasonsSetting
            on { appReviewLastPromptTimestamp } doReturn lastPromptSetting
            on { appReviewLastDeclineTimestamps } doReturn lastDeclineTimestampsSetting
            on { appReviewErrorSessionIds } doReturn errorSessionsSetting
            on { appReviewCrashTimestamp } doReturn crashTimestampSetting
        },
        loopIdleDuration = loopIdleDuration,
    )

    @Test
    fun `dispatch third episode completed reason`() = runTest {
        testInLoop {
            episodesCompletedSetting.set(List(1) { clock.instant() })
            expectNoSignal()

            episodesCompletedSetting.set(List(2) { clock.instant() })
            expectNoSignal()

            episodesCompletedSetting.set(List(3) { clock.instant() })
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `dispatch episode starred reason`() = runTest {
        testInLoop {
            episodeStarredSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.EpisodeStarred, signal.reason)
        }
    }

    @Test
    fun `dispatch show rated reason`() = runTest {
        testInLoop {
            podcastRatedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.ShowRated, signal.reason)
        }
    }

    @Test
    fun `dispatch filter created reason`() = runTest {
        testInLoop {
            playlistCreatedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.FilterCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch plus upgraded reason`() = runTest {
        testInLoop {
            plusUpgradedSetting.set(clock.instant())
            expectNoSignal()

            clock += 2.days
            expectNoSignal()

            clock += 1.seconds
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.PlusUpgraded, signal.reason)
        }
    }

    @Test
    fun `dispatch folder created reason`() = runTest {
        testInLoop {
            folderCreatedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.FolderCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch bookmark created reason`() = runTest {
        testInLoop {
            bookmarkCreatedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.BookmarkCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch custom theme set reason`() = runTest {
        testInLoop {
            themeChangedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.CustomThemeSet, signal.reason)
        }
    }

    @Test
    fun `dispatch referrals shared reason`() = runTest {
        testInLoop {
            referralSharedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.ReferralShared, signal.reason)
        }
    }

    @Test
    fun `dispatch playback shared reason`() = runTest {
        testInLoop {
            playbackSharedSetting.set(clock.instant())
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.PlaybackShared, signal.reason)
        }
    }

    @Test
    fun `do not dispatch the same event twice if consumed`() = runTest {
        testInLoop {
            episodesCompletedSetting.set(List(3) { clock.instant() })
            awaitSignalAndConsume()

            clock += 1.seconds // Move the clock so the values are different
            episodesCompletedSetting.set(List(3) { clock.instant() })
            episodesCompletedSetting.set(List(4) { clock.instant() })
            expectNoSignal()
        }
    }

    @Test
    fun `dispatch the same event again if not consumed`() = runTest {
        testInLoop {
            episodesCompletedSetting.set(List(3) { clock.instant() })
            awaitSignalAndIgnore()

            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `dispatch event only if the prompt was not triggered in 30 days`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        testInLoop {
            val trigger = launch { manager.triggerPrompt(AppReviewReason.DevelopmentTrigger) }
            awaitSignalAndConsume()
            trigger.join()

            episodesCompletedSetting.set(List(3) { clock.instant() })
            expectNoSignal()

            clock += 30.days
            expectNoSignal()

            clock += 1.days
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `dispatch event only if the prompt was not declined twice in 60 days`() = runTest {
        testInLoop {
            lastDeclineTimestampsSetting.set(
                listOf(
                    clock.instant().minusMillis(61.days.inWholeMilliseconds),
                    clock.instant(),
                ),
            )
            episodeStarredSetting.set(clock.instant())
            awaitSignalAndIgnore()

            lastDeclineTimestampsSetting.set(
                listOf(
                    clock.instant().minusMillis(60.days.inWholeMilliseconds),
                    clock.instant(),
                ),
            )
            episodeStarredSetting.set(clock.instant())
            expectNoSignal()
        }
    }

    @Test
    fun `dispatch event only if error occurred in the last 2 sessions`() = runTest {
        testInLoop {
            sessionIds.add("1")
            errorSessionsSetting.set(listOf("1"))

            episodeStarredSetting.set(clock.instant())
            expectNoSignal()

            sessionIds.add("2")
            expectNoSignal()

            sessionIds.add("3")
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.EpisodeStarred, signal.reason)
        }
    }

    @Test
    fun `dispatch event only if there was no crash in a week`() = runTest {
        testInLoop {
            crashTimestampSetting.set(clock.instant())

            episodeStarredSetting.set(clock.instant())
            expectNoSignal()

            clock += 7.days
            expectNoSignal()

            clock += 1.seconds
            val signal = awaitSignalAndConsume()
            assertEquals(AppReviewReason.EpisodeStarred, signal.reason)
        }
    }

    @Test
    fun `do not monitor if all reasons were dispatched`() = runTest {
        submittedReasonsSetting.set(AppReviewReason.entries - AppReviewReason.DevelopmentTrigger)

        val job = launch { manager.monitorAppReviewReasons() }
        yield()
        assertTrue(job.isCompleted)
    }

    @Test
    fun `do not monitor if user declined twice in 60 days`() = runTest {
        lastDeclineTimestampsSetting.set(listOf(clock.instant(), clock.instant()))

        val job = launch { manager.monitorAppReviewReasons() }
        yield()
        assertTrue(job.isCompleted)
    }

    private suspend fun testInLoop(validate: suspend LoopContext.() -> Unit) {
        manager.showPromptSignal.test {
            val monitoringJob = launch(start = CoroutineStart.UNDISPATCHED) { manager.monitorAppReviewReasons() }

            val loopContext = LoopContext(this, loopIdleDuration)
            loopContext.validate()

            monitoringJob.cancelAndJoin()
        }
    }

    private class LoopContext(
        private val turbineContext: TurbineTestContext<AppReviewSignal>,
        private val loopIdleDuration: Duration,
    ) {
        suspend fun TestScope.awaitSignalAndConsume(): AppReviewSignal {
            runLoopCycle()
            return turbineContext.awaitItem().also { it.consume() }
        }

        suspend fun TestScope.awaitSignalAndIgnore(): AppReviewSignal {
            runLoopCycle()
            return turbineContext.awaitItem().also { it.ignore() }
        }

        suspend fun TestScope.expectNoSignal() {
            runLoopCycle()
            turbineContext.expectNoEvents()
        }

        private suspend fun TestScope.runLoopCycle() {
            advanceTimeBy(loopIdleDuration)
            yield()
        }
    }
}

private class TestSetting<T>(
    initialValue: T,
) : ReadWriteSetting<T> {
    private val stateFlow = MutableStateFlow(initialValue)

    override val value: T
        get() = stateFlow.value

    override val flow: StateFlow<T>
        get() = stateFlow

    override fun set(value: T, updateModifiedAt: Boolean, commit: Boolean, clock: Clock) {
        stateFlow.value = value
    }

    fun set(value: T) = set(value, updateModifiedAt = false)
}
