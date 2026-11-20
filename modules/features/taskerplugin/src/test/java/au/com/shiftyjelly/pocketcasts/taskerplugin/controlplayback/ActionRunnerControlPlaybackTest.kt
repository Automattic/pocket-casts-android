package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import au.com.shiftyjelly.pocketcasts.taskerplugin.TaskerShimApplication
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback.PlaybackCommand
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TaskerShimApplication::class)
class ActionRunnerControlPlaybackTest {
    private val application = RuntimeEnvironment.getApplication() as TaskerShimApplication
    private val sleepTimer = application.sleepTimer

    @Test
    fun `set sleep timer starts the timer for the given minutes`() {
        val result = run(PlaybackCommand.SetSleepTimer, sleepTimerMinutes = "15")

        assertTrue(result is TaskerPluginResultSucess)
        assertTrue(sleepTimer.state.isSleepTimerRunning)
        assertEquals(15.minutes, sleepTimer.state.timeLeft)
    }

    @Test
    fun `set sleep timer replaces an end of episode timer`() {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = 2)

        run(PlaybackCommand.SetSleepTimer, sleepTimerMinutes = "30")

        assertEquals(0, sleepTimer.state.numberOfEpisodesLeft)
        assertEquals(30.minutes, sleepTimer.state.timeLeft)
    }

    @Test
    fun `set sleep timer rejects invalid minutes`() {
        listOf(null, "", "abc", "1.5", "0", "-5").forEach { minutes ->
            val result = run(PlaybackCommand.SetSleepTimer, sleepTimerMinutes = minutes)

            assertTrue("Expected an error for '$minutes'", result is TaskerPluginResultError)
            assertEquals("Sleep timer minutes $minutes not valid", (result as TaskerPluginResultError).message)
            assertFalse(sleepTimer.state.isSleepTimerRunning)
        }
    }

    @Test
    fun `cancel sleep timer stops a running timer`() {
        sleepTimer.sleepAfter(10.minutes)

        val result = run(PlaybackCommand.CancelSleepTimer)

        assertTrue(result is TaskerPluginResultSucess)
        assertFalse(sleepTimer.state.isSleepTimerRunning)
        assertEquals(ZERO, sleepTimer.state.timeLeft)
    }

    @Test
    fun `cancel sleep timer stops an end of episode timer`() {
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = 1)

        run(PlaybackCommand.CancelSleepTimer)

        assertFalse(sleepTimer.state.isSleepTimerRunning)
        assertEquals(0, sleepTimer.state.numberOfEpisodesLeft)
    }

    private fun run(command: PlaybackCommand, sleepTimerMinutes: String? = null): TaskerPluginResult<Unit> {
        val input = InputControlPlayback(command = command.name, sleepTimerMinutes = sleepTimerMinutes)
        return ActionRunnerControlPlayback().run(application, TaskerInput(input))
    }
}
