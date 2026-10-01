package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TouchDetectionFrameLayoutTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val root = TouchDetectionFrameLayout(context)
    private val container = FrameLayout(context)
    private val row = LinearLayout(context)
    private val leftChild = RecordingView(context)
    private val rightChild = RecordingView(context)

    init {
        row.addView(leftChild, LinearLayout.LayoutParams(WIDTH / 2, HEIGHT))
        row.addView(rightChild, LinearLayout.LayoutParams(WIDTH / 2, HEIGHT))
        container.addView(row, ViewGroup.LayoutParams(WIDTH, HEIGHT))
        val wrapper = FrameLayout(context)
        wrapper.addView(container, ViewGroup.LayoutParams(WIDTH, HEIGHT))
        root.addView(wrapper, ViewGroup.LayoutParams(WIDTH, HEIGHT))
    }

    @Test
    fun `recovers when the dispatching parent is removed mid gesture`() {
        rightChild.onUp = { container.removeView(row) }
        layOut()

        twoFingerTapReleasingRightFirst()

        assertFalse(root.isTouching)
        assertTrue(MotionEvent.ACTION_CANCEL in leftChild.actions)

        val replacement = RecordingView(context)
        container.addView(replacement, ViewGroup.LayoutParams(WIDTH, HEIGHT))
        layOut()
        tap(LEFT_X)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), replacement.actions)
    }

    @Test
    fun `recovers when a sibling touch target is removed mid gesture`() {
        rightChild.onUp = { row.removeView(leftChild) }
        layOut()

        twoFingerTapReleasingRightFirst()
        rightChild.actions.clear()
        tap(RIGHT_X)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), rightChild.actions)
    }

    @Test(expected = NullPointerException::class)
    fun `rethrows unrelated null pointer exceptions`() {
        leftChild.onDown = { throw NullPointerException() }
        layOut()

        tap(LEFT_X)
    }

    @Test
    fun `matches crash reported from the inlined dispatch frame`() {
        val exception = NullPointerException(
            "Attempt to read from field 'int android.view.View.mPrivateFlags' on a null object reference",
        )
        exception.stackTrace = arrayOf(
            StackTraceElement("android.view.ViewGroup", "dispatchTouchEvent", "ViewGroup.java", 2821),
        )

        assertTrue(exception.isRecycledTouchTargetCrash())
    }

    @Test
    fun `ignores crash from other classes`() {
        val exception = NullPointerException("mPrivateFlags")
        exception.stackTrace = arrayOf(StackTraceElement("com.example.Foo", "resetCancelNextUpFlag", "Foo.kt", 1))

        assertFalse(exception.isRecycledTouchTargetCrash())
    }

    private fun layOut() {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, WIDTH, HEIGHT)
    }

    private fun tap(x: Float) {
        val time = SystemClock.uptimeMillis()
        root.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, CENTER_Y, 0))
        root.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, x, CENTER_Y, 0))
    }

    private fun twoFingerTapReleasingRightFirst() {
        val first = pointer(0)
        val second = pointer(1)
        val left = coords(LEFT_X)
        val right = coords(RIGHT_X)
        val secondPointer = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
        dispatch(MotionEvent.ACTION_DOWN, arrayOf(first), arrayOf(left))
        dispatch(MotionEvent.ACTION_POINTER_DOWN or secondPointer, arrayOf(first, second), arrayOf(left, right))
        dispatch(MotionEvent.ACTION_POINTER_UP or secondPointer, arrayOf(first, second), arrayOf(left, right))
        dispatch(MotionEvent.ACTION_UP, arrayOf(first), arrayOf(left))
    }

    private fun dispatch(
        action: Int,
        pointers: Array<MotionEvent.PointerProperties>,
        coords: Array<MotionEvent.PointerCoords>,
    ) {
        val time = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(time, time, action, pointers.size, pointers, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        root.dispatchTouchEvent(event)
    }

    private fun pointer(pointerId: Int) = MotionEvent.PointerProperties().apply {
        id = pointerId
        toolType = MotionEvent.TOOL_TYPE_FINGER
    }

    private fun coords(pointerX: Float) = MotionEvent.PointerCoords().apply {
        x = pointerX
        y = CENTER_Y
    }

    private class RecordingView(context: Context) : View(context) {
        val actions = mutableListOf<Int>()
        var onDown: () -> Unit = {}
        var onUp: () -> Unit = {}

        override fun onTouchEvent(event: MotionEvent): Boolean {
            actions += event.actionMasked
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> onDown()
                MotionEvent.ACTION_UP -> onUp()
            }
            return true
        }
    }

    private companion object {
        const val WIDTH = 200
        const val HEIGHT = 100
        const val LEFT_X = 50f
        const val RIGHT_X = 150f
        const val CENTER_Y = 50f
    }
}
