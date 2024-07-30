package au.com.shiftyjelly.pocketcasts.views.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * A [NestedScrollView] that can be locked to prevent scrolling.
 * This is useful when a scrollable compose view is nested inside a scroll view in a bottom sheet.
 * Locking the scroll view, allows scrolling within the compose view.
 */
class LockableNestedScrollView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : NestedScrollView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var scrollable = true

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return scrollable && super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return scrollable && super.onTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        return scrollable && super.performClick()
    }

    fun setScrollingEnabled(enabled: Boolean) {
        scrollable = enabled
    }
}
