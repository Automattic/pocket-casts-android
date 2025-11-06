package au.com.shiftyjelly.pocketcasts.repositories.appreview

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AppReviewReason
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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

    private val submittedReasonsSetting = TestSetting(emptyList<AppReviewReason>())
    private val lastPromptSetting = TestSetting<Instant?>(null)

    private val clock = MutableClock()

    private val manager = AppReviewManagerImpl(
        clock = clock,
        settings = mock<Settings> {
            on { appReviewEpisodeCompletedTimestamps } doReturn episodesCompletedSetting
            on { appReviewEpisodeStarredTimestamp } doReturn episodeStarredSetting
            on { appReviewPodcastRatedTimestamp } doReturn podcastRatedSetting
            on { appReviewPlaylistCreatedTimestamp } doReturn playlistCreatedSetting
            on { appReviewPlusUpgradedTimestamp } doReturn plusUpgradedSetting
            on { appReviewFolderCreatedTimestamp } doReturn folderCreatedSetting
            on { appReviewBookmarkCreatedTimestamp } doReturn bookmarkCreatedSetting
            on { appReviewThemeChangedTimestamp } doReturn themeChangedSetting
            on { appReviewReferralSharedTimestamp } doReturn referralSharedSetting
            on { appReviewSubmittedReasons } doReturn submittedReasonsSetting
            on { appReviewLastPromptTimestamp } doReturn lastPromptSetting
        },
    )

    @Test
    fun `dispatch third episode completed reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            episodesCompletedSetting.set(List(1) { clock.instant() })
            expectNoEvents()

            episodesCompletedSetting.set(List(2) { clock.instant() })
            expectNoEvents()

            episodesCompletedSetting.set(List(3) { clock.instant() })
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `dispatch episode starred reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            episodeStarredSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.EpisodeStarred, signal.reason)
        }
    }

    @Test
    fun `dispatch show rated reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            podcastRatedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.ShowRated, signal.reason)
        }
    }

    @Test
    fun `dispatch filter created reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            playlistCreatedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.FilterCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch plus upgraded reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            plusUpgradedSetting.set(clock.instant())
            expectNoEvents()

            clock += 2.days
            expectNoEvents()

            clock += 1.seconds
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.PlusUpgraded, signal.reason)
        }
    }

    @Test
    fun `dispatch folder created reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            folderCreatedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.FolderCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch bookmark created reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            bookmarkCreatedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.BookmarkCreated, signal.reason)
        }
    }

    @Test
    fun `dispatch custom theme set reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            themeChangedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.CustomThemeSet, signal.reason)
        }
    }

    @Test
    fun `dispatch referrals shared reason`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            referralSharedSetting.set(clock.instant())
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.ReferralShared, signal.reason)
        }
    }

    @Test
    fun `do not dispatch the same event twice if consumed`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            episodesCompletedSetting.set(List(3) { clock.instant() })
            awaitItem().consume()

            clock += 1.seconds // Move the clock so the values are different
            episodesCompletedSetting.set(List(3) { clock.instant() })
            episodesCompletedSetting.set(List(4) { clock.instant() })
            expectNoEvents()
        }
    }

    @Test
    fun `dispatch the same event again if not consumed`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            episodesCompletedSetting.set(List(3) { clock.instant() })
            awaitItem().ignore()

            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `dispatch event only if the prompt was not triggered in 30 days`() = runTest {
        backgroundScope.launch { manager.monitorAppReviewReasons() }

        manager.showPromptSignal.test {
            val trigger = launch { manager.triggerPrompt(AppReviewReason.DevelopmentTrigger) }
            awaitItem().consume()
            trigger.join()

            episodesCompletedSetting.set(List(3) { clock.instant() })
            expectNoEvents()

            clock += 50.days
            expectNoEvents()

            clock += 1.days
            val signal = awaitItem()
            signal.consume()
            assertEquals(AppReviewReason.ThirdEpisodeCompleted, signal.reason)
        }
    }

    @Test
    fun `do not monitor if all reasons were dispatched`() = runTest {
        submittedReasonsSetting.set(AppReviewReason.entries - AppReviewReason.DevelopmentTrigger)

        val job = launch { manager.monitorAppReviewReasons() }
        yield()
        assertTrue(job.isCompleted)
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
