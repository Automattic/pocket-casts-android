package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import java.time.Instant
import kotlin.time.Duration
import timber.log.Timber

class TouchDetectionFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    var isTouching = false
        private set

    private var recentReleaseTimestamp: Instant? = null

    fun wasTouchedInLast(duration: Duration): Boolean {
        val timestamp = recentReleaseTimestamp
        return isTouching || (timestamp != null && timestamp.isAfter(Instant.now().minusMillis(duration.inWholeMilliseconds)))
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                recentReleaseTimestamp = Instant.now()
            }
        }
        return try {
            super.dispatchTouchEvent(event)
        } catch (e: NullPointerException) {
            if (!e.isRecycledTouchTargetCrash()) {
                throw e
            }
            Timber.w(e, "Touch target removed during touch dispatch")
            cancelTouchTargets(event)
            true
        }
    }

    private fun cancelTouchTargets(event: MotionEvent) {
        val cancelEvent = MotionEvent.obtain(event)
        cancelEvent.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }
}

internal fun NullPointerException.isRecycledTouchTargetCrash(): Boolean {
    val frame = stackTrace.firstOrNull() ?: return false
    if (frame.className != "android.view.ViewGroup") {
        return false
    }
    return frame.methodName == "resetCancelNextUpFlag" ||
        (frame.methodName == "dispatchTouchEvent" && message.orEmpty().contains("mPrivateFlags"))
}
