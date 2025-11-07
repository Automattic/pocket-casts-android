package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import java.time.Instant
import kotlin.time.Duration

class TouchDetectionFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    var isTouching = false
        private set

    private var recentReleaseTimestamp: Instant? = null

    fun wasTouchedInLast(duration: Duration): Boolean {
        val timestamp = recentReleaseTimestamp
        return isTouching || timestamp != null && timestamp.isAfter(Instant.now().minusMillis(duration.inWholeMilliseconds))
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
        return super.dispatchTouchEvent(event)
    }
}
