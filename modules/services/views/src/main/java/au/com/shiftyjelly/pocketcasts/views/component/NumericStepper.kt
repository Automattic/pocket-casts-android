package au.com.shiftyjelly.pocketcasts.views.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import au.com.shiftyjelly.pocketcasts.views.R
import dagger.hilt.android.internal.managers.FragmentComponentManager
import java.util.Timer
import java.util.TimerTask

@SuppressLint("ClickableViewAccessibility")
class NumericStepper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnTouchListener {

    var initialDelay = 1000L
    var repeatDelay = 150L
    var maxValue = Int.MAX_VALUE
    var minValue = 0
    var largeStep = 5
    var smallStep = 1
    var setValues: List<Int>? = null

    var value = 0
        set(value) {
            field = value.coerceIn(minValue, maxValue)
            lblValue.text = formatter(field)
            lblValue.contentDescription = voiceOverFormatter(field)
            onValueChanged?.invoke(field)

            lblValue.announceForAccessibility(lblValue.contentDescription)
        }

    var onValueChanged: ((Int) -> Unit)? = null
    var formatter: (Int) -> String = { it.toString() }
        set(value) {
            field = value
            lblValue.text = field(this.value)
        }

    var voiceOverFormatter: (Int) -> String = { formatter(it) }
        set(value) {
            field = value
            lblValue.contentDescription = field(this.value)
        }

    var voiceOverPrefix: String? = null
        set(value) {
            field = value
            btnMinus.contentDescription = "$value minus"
            btnPlus.contentDescription = "$value plus"
        }

    var tintColor: ColorStateList? = null
        set(value) {
            field = value
            btnMinus.imageTintList = value
            btnPlus.imageTintList = value
        }

    private val btnMinus: ImageView
    private val btnPlus: ImageView
    private val lblValue: TextView

    private var holdTimer: Timer? = null

    init {
        orientation = HORIZONTAL
        View.inflate(context, R.layout.layout_numeric_stepper, this)

        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        lblValue = findViewById(R.id.lblValue)

        btnPlus.setOnTouchListener(this)
        btnMinus.setOnTouchListener(this)

        lblValue.text = formatter(value)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        val alpha = if (enabled) 1.0f else 0.5f
        children.forEach {
            it.alpha = alpha
            it.isEnabled = enabled
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        holdTimer?.cancel()
    }

    private fun calculateNext(incrementing: Boolean): Int {
        if (setValues != null) return chooseNextFromSetValues(incrementing)

        val inc = if (incrementing) {
            if (value >= largeStep) {
                largeStep
            } else {
                smallStep
            }
        } else {
            if (value <= largeStep) {
                -smallStep
            } else {
                -largeStep
            }
        }
        return value + inc
    }

    private fun chooseNextFromSetValues(incrementing: Boolean): Int {
        val values = setValues ?: return 0
        val currentIndex = values.indexOf(value)
        val nextIndex = (if (incrementing) currentIndex + 1 else currentIndex - 1).coerceIn(0, values.size - 1)
        return values[nextIndex]
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val view = v ?: return false
        val action = event?.action ?: return false
        val incrementing = view == btnPlus
        val nextValue = calculateNext(incrementing = incrementing)

        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                holdTimer?.cancel()
                holdTimer = null
            }
            MotionEvent.ACTION_DOWN -> {
                view.isPressed = true // This makes sure the ripple effect still works
                value = nextValue
                holdTimer = Timer().apply { schedule(IncrementTimerTask(incrementing), initialDelay, repeatDelay) }
            }
        }

        return true
    }

    inner class IncrementTimerTask(val incrementing: Boolean) : TimerTask() {
        override fun run() {
            (FragmentComponentManager.findActivity(context) as? AppCompatActivity)?.runOnUiThread {
                this@NumericStepper.value = calculateNext(incrementing)
            }
        }
    }
}
