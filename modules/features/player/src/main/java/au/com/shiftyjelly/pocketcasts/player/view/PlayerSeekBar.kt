package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import me.zhanghai.android.materialprogressbar.MaterialProgressBar

class PlayerSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val view = inflater.inflate(R.layout.view_playback_seek_bar, this)
    private var seeking = false
    private var currentTimeMs: Int = 0
    private var durationMs: Int = 0
    private val seekBar = view.findViewById(R.id.seekBarInternal) as AppCompatSeekBar
    private val elapsedTimeText = view.findViewById(R.id.elapsedTime) as TextView
    private val remainingTimeText = view.findViewById(R.id.remainingTime) as TextView
    private val bufferingSeekbar = view.findViewById(R.id.indeterminateProgressBar) as MaterialProgressBar

    var bufferedUpToInSecs: Int = 0
        set(value) {
            field = value
            seekBar.secondaryProgress = value
        }

    var isBuffering: Boolean = false
        set(value) {
            field = value
            bufferingSeekbar.isVisible = value
        }

    var changeListener: OnUserSeekListener? = null

    init {
        setupSeekBar()
    }

    fun setCurrentTimeMs(currentTimeMs: Int) {
        if (seeking) {
            return
        }
        this.currentTimeMs = currentTimeMs
        seekBar.progress = currentTimeMs / 1000
        updateTextViews()
    }

    fun setDurationMs(durationMs: Int) {
        this.durationMs = durationMs
        val durationSecs = durationMs / 1000
        seekBar.max = durationSecs
        updateTextViews()
    }

    fun setTintColor(color: Int?, theme: Theme.ThemeType) {
        val themeColor = ThemeColor.playerHighlight01(theme, color ?: Color.WHITE)
        seekBar.thumbTintList = ColorStateList.valueOf(ThemeColor.playerContrast01(theme))
        seekBar.progressTintList = ColorStateList.valueOf(themeColor)
        seekBar.secondaryProgressTintList = ColorStateList.valueOf(ThemeColor.playerContrast05(theme))
        seekBar.backgroundTintList = ColorStateList.valueOf(ThemeColor.playerContrast05(theme))

        with(bufferingSeekbar) {
            supportIndeterminateTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(themeColor, 0x1A))
        }

        elapsedTimeText.setTextColor(ThemeColor.playerContrast02(theme))
        remainingTimeText.setTextColor(ThemeColor.playerContrast02(theme))
    }

    private fun setupSeekBar() {
        bufferingSeekbar.isVisible = false
        seekBar.secondaryProgress = 0
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                changeListener?.onSeekPositionChangeStop(currentTimeMs) {
                    seeking = false
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                changeListener?.onSeekPositionChangeStart()
                seeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar, progressSecs: Int, fromUser: Boolean) {
                if (!fromUser) return

                currentTimeMs = progressSecs * 1000
                updateTextViews()

                changeListener?.onSeekPositionChanging(currentTimeMs)
            }
        })
    }

    private fun updateTextViews() {
        if (durationMs <= 0) {
            elapsedTimeText.text = ""
            elapsedTimeText.contentDescription = ""
            remainingTimeText.text = ""
            remainingTimeText.contentDescription = ""
        } else {
            val elapsedTime = TimeHelper.formattedMs(currentTimeMs)
            elapsedTimeText.text = elapsedTime
            elapsedTimeText.contentDescription = "Played up to $elapsedTime"
            val timeRemaining = TimeHelper.getTimeLeftOnlyNumbers(currentTimeMs, durationMs)
            remainingTimeText.text = timeRemaining
            remainingTimeText.contentDescription = timeRemaining
        }
    }

    interface OnUserSeekListener {
        fun onSeekPositionChangeStop(progress: Int, seekComplete: () -> Unit)
        fun onSeekPositionChanging(progress: Int)
        fun onSeekPositionChangeStart()
    }
}
