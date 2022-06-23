package au.com.shiftyjelly.pocketcasts.player.helper

import android.animation.TimeInterpolator
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.LinearInterpolator

class BottomSheetAnimation(
    val viewId: Int,
    val rootView: ViewGroup?,
    val effect: Int,
    val slideOffsetFrom: Float,
    val slideOffsetTo: Float,
    val valueFrom: Float = 0f,
    val valueTo: Float = 1f,
    val minDiff: Float = 0.01f,
    val openStartDelay: Long = 0,
    val closeStartDelay: Long = 0,
    val openInterpolator: TimeInterpolator = LinearInterpolator(),
    val closeInterpolator: TimeInterpolator = LinearInterpolator(),
    val disabled: Boolean = false
) {

    companion object {
        const val ALPHA = 1
        const val SCALE = 2
        const val TRANSLATE_Y = 3

        const val ALPHA_INVISIBLE = 0f
        const val ALPHA_VISIBLE = 1f

        const val SCALE_NORMAL = 1f
        const val SCALE_GONE = 0f
    }

    private var view: View? = null
    private var lastValue = -1f
    private var settling = false
    private var dragging = false
    private var collapsed = true
    private val scaleMultiplier = 1f / (slideOffsetTo - slideOffsetFrom)
    private var animation: ViewPropertyAnimator? = null

    fun onSlide(offset: Float) {
        if (offset < slideOffsetFrom) {
            onValueChanged(0f)
            return
        }
        if (offset > slideOffsetTo) {
            onValueChanged(1f)
            return
        }

        val value = (offset - slideOffsetFrom) * scaleMultiplier
        onValueChanged(value)
    }

    private fun findView(): View? {
        if (view == null && rootView != null) {
            view = rootView.findViewById(viewId)
        }
        return view
    }

    private fun onValueChanged(value: Float) {
        val view = findView()
        if (disabled || view == null || value == lastValue || (settling && !dragging)) {
            return
        }
        val diff = Math.abs(lastValue - value)
        if (value != 0f && value != 1f && diff <= minDiff) {
            return
        }

        lastValue = value
        val actualValue = ((valueTo - valueFrom) * value) + valueFrom

        when (effect) {
            ALPHA -> view.animate().alpha(actualValue).setStartDelay(0).setDuration(0).start()
            SCALE -> view.animate().scaleX(actualValue).scaleY(actualValue).setStartDelay(0).setDuration(0).start()
            TRANSLATE_Y -> view.animate().translationY(actualValue).setStartDelay(0).setDuration(0).start()
        }
    }

    fun onCollapsed() {
        if (disabled) {
            return
        }
        collapsed = true
        settling = false
        dragging = false

        val view = findView() ?: return
        if (effect == ALPHA) {
            if (valueFrom == ALPHA_INVISIBLE) {
                view.visibility = View.GONE
            }
            if (valueFrom == ALPHA_VISIBLE) {
                view.visibility = View.VISIBLE
            }
        } else if (effect == TRANSLATE_Y) {
            view.translationY = valueFrom
        } else if (effect == SCALE) {
            view.scaleX = valueFrom
            view.scaleY = valueFrom
        }
    }

    fun onDragging() {
        if (disabled) {
            return
        }
        dragging = true
        val view = findView() ?: return
        if (effect == ALPHA) {
            view.visibility = View.VISIBLE
        }
    }

    fun onExpanded() {
        if (disabled) {
            return
        }
        collapsed = false
        settling = false
        dragging = false

        val view = findView() ?: return
        // fading out hide the view once i t is open
        if (effect == ALPHA) {
            if (valueTo == ALPHA_INVISIBLE) {
                view.visibility = View.GONE
            }
            if (valueTo == ALPHA_VISIBLE) {
                view.visibility = View.VISIBLE
            }
        } else if (effect == TRANSLATE_Y) {
            view.translationY = valueTo
        } else if (effect == SCALE) {
            view.scaleX = valueTo
            view.scaleY = valueTo
        }
    }

    fun onSettling() {
        settling = true
        val view = findView()
        if (view == null || disabled) {
            return
        }

        if (effect == ALPHA) {
            view.visibility = View.VISIBLE
        }

        if (!dragging) {
            // user tapped open
            if (collapsed) {
                animation = when (effect) {
                    ALPHA -> view.animate().alpha(valueTo).setStartDelay(openStartDelay).setDuration(300)
                    SCALE -> view.animate().scaleX(valueTo).scaleY(valueTo).setStartDelay(openStartDelay).setInterpolator(openInterpolator).setDuration(300)
                    TRANSLATE_Y -> view.animate().translationY(valueTo).setStartDelay(openStartDelay).setInterpolator(openInterpolator).setDuration(300)
                    else -> null
                }
            }
            // user tapped close
            else {
                animation = when (effect) {
                    ALPHA -> view.animate().alpha(valueFrom).setStartDelay(closeStartDelay).setDuration(300)
                    SCALE -> view.animate().scaleX(valueFrom).scaleY(valueFrom).setStartDelay(closeStartDelay).setDuration(300).setInterpolator(closeInterpolator)
                    TRANSLATE_Y -> view.animate().translationY(valueFrom).setStartDelay(closeStartDelay).setDuration(300).setInterpolator(closeInterpolator)
                    else -> null
                }
            }

            animation?.start()
        }
    }
}
