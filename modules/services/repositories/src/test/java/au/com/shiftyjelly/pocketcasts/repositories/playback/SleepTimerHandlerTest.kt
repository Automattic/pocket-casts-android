package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.Application
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlin.time.AbstractLongTimeSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SleepTimerHandlerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val sleepTimer = SleepTimer(settings = mock(), eventHorizon = mock())
    private val playbackManager = mock<PlaybackManager>()
    private val context = spy<Application>(RuntimeEnvironment.getApplication()) {
        doReturn("Sleep timer stopped").whenever(it).getString(R.string.player_sleep_timer_stopped_your_podcast)
    }

    private fun TestScope.createHandler(timeSource: TimeSource = testScheduler.timeSource): SleepTimerHandler {
        return SleepTimerHandler(sleepTimer, playbackManager, { context }, backgroundScope, timeSource).also {
            it.observe()
            runCurrent()
        }
    }

    private fun TestScope.advanceSeconds(seconds: Int) {
        advanceTimeBy(seconds.seconds)
        runCurrent()
    }

    @Test
    fun `ticks once per second after an initial one second delay`() = runTest {
        createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceTimeBy(999.milliseconds)
        runCurrent()
        assertEquals(10.seconds, sleepTimer.state.timeLeft)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(9.seconds, sleepTimer.state.timeLeft)

        advanceSeconds(3)
        assertEquals(6.seconds, sleepTimer.state.timeLeft)
    }

    @Test
    fun `fades out at five seconds and pauses when the timer runs out`() = runTest {
        createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()

        advanceSeconds(4)
        verify(playbackManager, never()).performVolumeFadeOut(any())

        advanceSeconds(1)
        verify(playbackManager).performVolumeFadeOut(5.0)
        verify(playbackManager, never()).pause(any(), any())

        advanceSeconds(5)
        verify(playbackManager).pause(sourceView = SourceView.AUTO_PAUSE)
        verify(playbackManager).restorePlayerVolume()
        assertFalse(sleepTimer.state.isSleepTimerRunning)
        assertEquals(0.seconds, sleepTimer.state.timeLeft)
    }

    @Test
    fun `a slow tick does not push back the pause`() = runTest {
        var slowTickTimeMs = 0L
        val clock = object : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
            override fun read() = testScheduler.currentTime + slowTickTimeMs
        }
        doAnswer { slowTickTimeMs += 500 }.whenever(playbackManager).performVolumeFadeOut(any())
        createHandler(clock)

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceSeconds(5)
        verify(playbackManager).performVolumeFadeOut(5.0)

        advanceTimeBy(4500.milliseconds)
        runCurrent()
        verify(playbackManager).pause(sourceView = SourceView.AUTO_PAUSE)
    }

    @Test
    fun `adding time while running keeps a single countdown`() = runTest {
        createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceSeconds(3)

        sleepTimer.addExtraTime(5.seconds)
        runCurrent()
        assertEquals(12.seconds, sleepTimer.state.timeLeft)

        advanceSeconds(1)
        assertEquals(11.seconds, sleepTimer.state.timeLeft)

        advanceSeconds(2)
        assertEquals(9.seconds, sleepTimer.state.timeLeft)
    }

    @Test
    fun `stopping the sleep timer cancels the countdown`() = runTest {
        createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceSeconds(3)

        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        runCurrent()
        advanceSeconds(20)

        assertFalse(sleepTimer.state.isSleepTimerRunning)
        verify(playbackManager, never()).performVolumeFadeOut(any())
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `restarting after a stop begins a fresh countdown`() = runTest {
        createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceTimeBy(1500.milliseconds)
        runCurrent()
        assertEquals(9.seconds, sleepTimer.state.timeLeft)

        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        runCurrent()
        sleepTimer.sleepAfter(4.seconds)
        runCurrent()

        advanceTimeBy(999.milliseconds)
        runCurrent()
        assertEquals(4.seconds, sleepTimer.state.timeLeft)

        advanceSeconds(4)
        verify(playbackManager).pause(sourceView = SourceView.AUTO_PAUSE)
    }

    @Test
    fun `dispose stops the countdown`() = runTest {
        val handler = createHandler()

        sleepTimer.sleepAfter(10.seconds)
        runCurrent()
        advanceSeconds(2)

        handler.dispose()
        advanceSeconds(20)

        assertTrue(sleepTimer.state.isSleepTimerRunning)
        assertEquals(8.seconds, sleepTimer.state.timeLeft)
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `an error during a tick is caught without failing the scope`() = runTest {
        doThrow(IllegalStateException("fade failed")).whenever(playbackManager).performVolumeFadeOut(any())
        createHandler()

        sleepTimer.sleepAfter(6.seconds)
        runCurrent()
        advanceSeconds(1)
        verify(playbackManager).performVolumeFadeOut(5.0)
        assertTrue(backgroundScope.isActive)

        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        runCurrent()
        sleepTimer.sleepAfter(3.seconds)
        runCurrent()
        advanceSeconds(3)
        verify(playbackManager).pause(sourceView = SourceView.AUTO_PAUSE)
    }
}
