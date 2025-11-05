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
    private val submittedReasonsSetting = TestSetting(emptyList<AppReviewReason>())
    private val lastPromptSetting = TestSetting(emptyList<Instant>())
    private val episodesCompletedSetting = TestSetting(emptyList<Instant>())

    private val clock = MutableClock()

    private val manager = AppReviewManagerImpl(
        clock = clock,
        settings = mock<Settings> {
            on { appReviewSubmittedReasons } doReturn submittedReasonsSetting
            on { appReviewPromptTimestamps } doReturn lastPromptSetting
            on { appReviewEpisodeCompletedTimestamps } doReturn episodesCompletedSetting
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
