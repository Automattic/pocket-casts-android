package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.util.Date
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SleepTimerTest {
    private val clock = MutableClock()
    private val autoRestartSetting = mock<UserSetting<Boolean>> { on { value } doReturn true }
    private val settings = mock<Settings> {
        on { autoSleepTimerRestart } doReturn autoRestartSetting
        on { getlastSleepEndOfEpisodes() } doReturn 2
        on { getlastSleepEndOfChapter() } doReturn 3
    }
    private val sleepTimer = SleepTimer(settings = settings, eventHorizon = mock(), clock = clock)

    @Test
    fun `restarts a sleep after time timer when playback resumes within five minutes of it running out`() {
        sleepTimer.sleepAfter(15.minutes)
        clock += 15.minutes
        runOutSleepAfterTime()

        clock += 4.minutes + 59.seconds
        sleepTimer.restartSleepTimerIfApplies(currentEpisodeUuid = "episode")

        assertTrue(sleepTimer.state.isSleepTimerRunning)
        assertEquals(15.minutes, sleepTimer.state.timeLeft)
    }

    @Test
    fun `does not restart a sleep after time timer when playback resumes five minutes after it ran out`() {
        sleepTimer.sleepAfter(15.minutes)
        clock += 15.minutes
        runOutSleepAfterTime()

        clock += 5.minutes
        sleepTimer.restartSleepTimerIfApplies(currentEpisodeUuid = "episode")

        assertFalse(sleepTimer.state.isSleepTimerRunning)
    }

    @Test
    fun `measures the restart window from when the timer ran out after time was added`() {
        sleepTimer.sleepAfter(10.minutes)
        sleepTimer.addExtraTime(5.minutes)
        clock += 15.minutes
        runOutSleepAfterTime()

        clock += 1.minutes
        sleepTimer.restartSleepTimerIfApplies(currentEpisodeUuid = "episode")

        assertTrue(sleepTimer.state.isSleepTimerRunning)
    }

    @Test
    fun `restarts an end of episode timer when playback resumes within five minutes`() = runTest {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = 1)
        sleepTimer.sleepEndOfEpisode(PodcastEpisode(uuid = "finished", publishedDate = Date())) {}

        clock += 4.minutes + 59.seconds
        sleepTimer.restartSleepTimerIfApplies(currentEpisodeUuid = "next")

        assertEquals(2, sleepTimer.state.numberOfEpisodesLeft)
    }

    @Test
    fun `restarts an end of chapter timer when playback resumes within five minutes`() = runTest {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterChapters = 1)
        sleepTimer.verifySleepTimeForEndOfChapter(currentChapterUuid = "chapter-1", currentEpisodeUuid = "episode") {}
        sleepTimer.verifySleepTimeForEndOfChapter(currentChapterUuid = "chapter-2", currentEpisodeUuid = "episode") {}

        clock += 4.minutes + 59.seconds
        sleepTimer.restartSleepTimerIfApplies(currentEpisodeUuid = "episode")

        assertEquals(3, sleepTimer.state.numberOfChaptersLeft)
    }

    // Mirrors what SleepTimerHandler does when the countdown reaches zero
    private fun runOutSleepAfterTime() {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        sleepTimer.recordSleepAfterTimeFinished()
    }
}
