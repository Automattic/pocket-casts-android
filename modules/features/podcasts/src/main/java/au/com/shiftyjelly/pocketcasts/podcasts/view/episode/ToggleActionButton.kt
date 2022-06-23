package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground

class ToggleActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    sealed class State(@StringRes val textRes: Int, @DrawableRes val iconRes: Int) {
        class Off(textRes: Int, iconRes: Int) : State(textRes, iconRes)
        class On(textRes: Int, iconRes: Int) : State(textRes, iconRes)
    }

    private val imgIcon: ImageView
    private val lblStatus: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.podcasts_toggle_action_button, this, true)
        imgIcon = findViewById(R.id.imgIcon)
        lblStatus = findViewById(R.id.lblStatus)
        orientation = VERTICAL
        setRippleBackground(borderless = true)

        setOnClickListener {
            // Toggle
            state = if (state is State.On) {
                offState
            } else {
                onState
            }

            onStateChange?.invoke(state!!)
        }
    }

    lateinit var offState: State.Off
    lateinit var onState: State.On
    var onStateChange: ((State) -> Unit)? = null

    var state: State? = null
        set(value) {
            field = value
            value?.let {
                updateUI(value)
            }
        }

    @ColorInt
    var tintColor: Int = 0
        set(value) {
            ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(value))
            field = value
        }

    var isOn: Boolean
        get() = state == onState
        set(value) {
            state = if (value) onState else offState
        }

    fun setup(onState: State.On, offState: State.Off, isOn: Boolean) {
        this.onState = onState
        this.offState = offState

        if (isOn) {
            updateUI(onState)
        } else {
            updateUI(offState)
        }
    }

    private fun updateUI(state: State) {
        imgIcon.setImageResource(state.iconRes)
        lblStatus.setText(state.textRes)
    }
}
