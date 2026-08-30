package au.com.shiftyjelly.pocketcasts.player.view

import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import au.com.shiftyjelly.pocketcasts.player.R
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class PlayerSeekBarTest {
    private lateinit var activityController: ActivityController<Activity>
    private lateinit var activity: Activity
    private lateinit var playerSeekBar: PlayerSeekBar
    private lateinit var seekBar: SeekBar
    private lateinit var listener: RecordingSeekListener

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        activity = activityController.get()
        listener = RecordingSeekListener()
        playerSeekBar = PlayerSeekBar(activity).apply {
            setDuration(120.seconds)
            setEpisodeUuid(EPISODE_ONE)
            changeListener = listener
        }
        activity.setContentView(playerSeekBar)
        seekBar = playerSeekBar.findViewById(R.id.seekBarInternal)
        seekBar.measure(
            View.MeasureSpec.makeMeasureSpec(SEEK_BAR_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        seekBar.layout(0, 0, seekBar.measuredWidth, seekBar.measuredHeight)
    }

    @After
    fun tearDown() {
        activityController.pause().stop().destroy()
    }

    @Test
    fun `accessibility progress changes are debounced and coalesced to the latest target`() {
        setProgressWithAccessibility(10)

        assertEquals(1, listener.startCount)
        assertEquals(listOf(10.seconds), listener.changingProgress)
        assertTrue(listener.stopRequests.isEmpty())

        idleMainLooperFor(500)
        setProgressWithAccessibility(25)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS - 1)

        assertEquals(1, listener.startCount)
        assertEquals(listOf(10.seconds, 25.seconds), listener.changingProgress)
        assertTrue(listener.stopRequests.isEmpty())

        idleMainLooperFor(1)

        assertEquals(1, listener.stopRequests.size)
        assertEquals(EPISODE_ONE, listener.stopRequests.single().episodeUuid)
        assertEquals(25.seconds, listener.stopRequests.single().progress)
    }

    @Test
    fun `stale completion cannot clear a newer seek session`() {
        setProgressWithAccessibility(10)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)
        val firstRequest = listener.stopRequests.single()

        setProgressWithAccessibility(30)
        assertEquals(2, listener.startCount)

        firstRequest.complete()
        idleMainLooper()
        playerSeekBar.setCurrentTime(50.seconds)

        assertEquals(30, seekBar.progress)

        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(2, listener.stopRequests.size)
        assertEquals(30.seconds, listener.stopRequests.last().progress)
    }

    @Test
    fun `episode change flushes the pending seek with the old episode and accepts new playback state`() {
        setProgressWithAccessibility(35)

        playerSeekBar.setEpisodeUuid(EPISODE_TWO)
        playerSeekBar.setCurrentTime(7.seconds)

        assertEquals(1, listener.stopRequests.size)
        assertEquals(EPISODE_ONE, listener.stopRequests.single().episodeUuid)
        assertEquals(35.seconds, listener.stopRequests.single().progress)
        assertEquals(7, seekBar.progress)

        listener.stopRequests.single().complete()
        idleMainLooper()
        playerSeekBar.setCurrentTime(9.seconds)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(1, listener.stopRequests.size)
        assertEquals(9, seekBar.progress)
    }

    @Test
    fun `detach flushes a pending non-touch seek`() {
        setProgressWithAccessibility(45)
        assertTrue(playerSeekBar.isAttachedToWindow)

        activity.findViewById<ViewGroup>(android.R.id.content).removeView(playerSeekBar)

        assertEquals(1, listener.stopRequests.size)
        assertEquals(EPISODE_ONE, listener.stopRequests.single().episodeUuid)
        assertEquals(45.seconds, listener.stopRequests.single().progress)

        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(1, listener.stopRequests.size)
    }

    @Test
    fun `reattached seek bar accepts accessibility input after cancelling a touch`() {
        val downTime = SystemClock.uptimeMillis()
        dispatchTouch(MotionEvent.ACTION_DOWN, 15, downTime, eventTimeOffsetMs = 0)
        dispatchTouch(MotionEvent.ACTION_MOVE, 30, downTime, eventTimeOffsetMs = 16)

        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.removeView(playerSeekBar)
        content.addView(playerSeekBar)
        setProgressWithAccessibility(45)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(1, listener.cancelCount)
        assertEquals(2, listener.startCount)
        assertEquals(1, listener.stopRequests.size)
        assertEquals(45.seconds, listener.stopRequests.single().progress)
    }

    @Test
    fun `playback updates remain blocked until seek completion is processed`() {
        setProgressWithAccessibility(20)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)
        val request = listener.stopRequests.single()

        playerSeekBar.setCurrentTime(40.seconds)
        assertEquals(20, seekBar.progress)

        request.complete()
        playerSeekBar.setCurrentTime(50.seconds)
        assertEquals(20, seekBar.progress)

        idleMainLooper()
        playerSeekBar.setCurrentTime(60.seconds)

        assertEquals(60, seekBar.progress)
    }

    @Test
    fun `touch takes over a pending accessibility seek without duplicate callbacks`() {
        setProgressWithAccessibility(10)

        val downTime = SystemClock.uptimeMillis()
        dispatchTouch(MotionEvent.ACTION_DOWN, 20, downTime, eventTimeOffsetMs = 0)
        dispatchTouch(MotionEvent.ACTION_MOVE, 40, downTime, eventTimeOffsetMs = 16)
        dispatchTouch(MotionEvent.ACTION_UP, 40, downTime, eventTimeOffsetMs = 32)

        assertEquals(1, listener.startCount)
        assertEquals(1, listener.stopRequests.size)
        assertEquals(0, listener.cancelCount)
        assertEquals(EPISODE_ONE, listener.stopRequests.single().episodeUuid)
        assertEquals(listener.changingProgress.last(), listener.stopRequests.single().progress)

        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(1, listener.startCount)
        assertEquals(1, listener.stopRequests.size)
    }

    @Test
    fun `episode change cancels the rest of the active gesture and a fresh gesture can seek`() {
        val oldGestureDownTime = SystemClock.uptimeMillis()
        dispatchTouch(MotionEvent.ACTION_DOWN, 15, oldGestureDownTime, eventTimeOffsetMs = 0)
        dispatchTouch(MotionEvent.ACTION_MOVE, 30, oldGestureDownTime, eventTimeOffsetMs = 16)
        val changingCountAtEpisodeChange = listener.changingProgress.size

        playerSeekBar.setEpisodeUuid(EPISODE_TWO)
        dispatchTouch(MotionEvent.ACTION_MOVE, 50, oldGestureDownTime, eventTimeOffsetMs = 32)
        dispatchTouch(MotionEvent.ACTION_UP, 50, oldGestureDownTime, eventTimeOffsetMs = 48)
        idleMainLooperFor(NON_TOUCH_COMMIT_DELAY_MS)

        assertEquals(1, listener.startCount)
        assertEquals(1, listener.cancelCount)
        assertEquals(changingCountAtEpisodeChange, listener.changingProgress.size)
        assertTrue(listener.stopRequests.isEmpty())

        val newGestureDownTime = SystemClock.uptimeMillis()
        dispatchTouch(MotionEvent.ACTION_DOWN, 60, newGestureDownTime, eventTimeOffsetMs = 0)
        dispatchTouch(MotionEvent.ACTION_MOVE, 75, newGestureDownTime, eventTimeOffsetMs = 16)
        dispatchTouch(MotionEvent.ACTION_UP, 75, newGestureDownTime, eventTimeOffsetMs = 32)

        assertEquals(2, listener.startCount)
        assertEquals(1, listener.cancelCount)
        assertEquals(1, listener.stopRequests.size)
        assertEquals(EPISODE_TWO, listener.stopRequests.single().episodeUuid)
        assertEquals(listener.changingProgress.last(), listener.stopRequests.single().progress)
    }

    private fun setProgressWithAccessibility(progressSecs: Int) {
        val arguments = Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, progressSecs.toFloat())
        }
        val handled = seekBar.performAccessibilityAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
            arguments,
        )
        assertTrue("Accessibility progress action was not handled", handled)
    }

    private fun dispatchTouch(action: Int, progressSecs: Int, downTime: Long, eventTimeOffsetMs: Long) {
        val trackWidth = seekBar.width - seekBar.paddingLeft - seekBar.paddingRight
        val x = seekBar.paddingLeft + trackWidth * progressSecs.toFloat() / seekBar.max
        val event = MotionEvent.obtain(
            downTime,
            downTime + eventTimeOffsetMs,
            action,
            x,
            seekBar.height / 2f,
            0,
        )
        try {
            assertTrue("Touch action $action was not handled", seekBar.dispatchTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun idleMainLooperFor(durationMs: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(durationMs, TimeUnit.MILLISECONDS)
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class RecordingSeekListener : PlayerSeekBar.OnUserSeekListener {
        var startCount = 0
        val changingProgress = mutableListOf<Duration>()
        val stopRequests = mutableListOf<StopRequest>()
        var cancelCount = 0

        override fun onSeekPositionChangeStop(
            episodeUuid: String,
            progress: Duration,
            seekComplete: () -> Unit,
        ) {
            stopRequests += StopRequest(episodeUuid, progress, seekComplete)
        }

        override fun onSeekPositionChanging(progress: Duration) {
            changingProgress += progress
        }

        override fun onSeekPositionChangeStart() {
            startCount++
        }

        override fun onSeekPositionChangeCancel() {
            cancelCount++
        }
    }

    private data class StopRequest(
        val episodeUuid: String,
        val progress: Duration,
        val complete: () -> Unit,
    )

    private companion object {
        const val EPISODE_ONE = "episode-one"
        const val EPISODE_TWO = "episode-two"
        const val NON_TOUCH_COMMIT_DELAY_MS = 750L
        const val SEEK_BAR_WIDTH_PX = 1_000
    }
}
