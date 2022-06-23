package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import au.com.shiftyjelly.pocketcasts.podcasts.R

class PlaybackSpeedPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle) : Preference(context, attrs, defStyleAttr) {

    init {
        widgetLayoutResource = R.layout.preference_playback_speed
    }

    var onSpeedMinusClicked: (() -> Unit)? = null
    var onSpeedPlusClicked: (() -> Unit)? = null

    private var speedLabel: TextView? = null
    var speed: Double = 1.0
        set(value) {
            field = value
            updateSpeedLabel()
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // disable parent click
        holder.itemView.isClickable = false
        val buttonDown = holder.findViewById(R.id.btnSpeedDown)
        buttonDown.setOnClickListener {
            onSpeedMinusClicked?.invoke()
        }
        val upDown = holder.findViewById(R.id.btnSpeedUp)
        upDown.setOnClickListener {
            onSpeedPlusClicked?.invoke()
        }
        speedLabel = holder.findViewById(R.id.lblSpeed) as TextView
        updateSpeedLabel()
    }

    private fun updateSpeedLabel() {
        speedLabel?.text = String.format("%.1fx", speed)
    }
}
