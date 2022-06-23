package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.component.ProgressCircleView
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import java.lang.Math.round
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class DownloadButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imgIcon: ImageView
    private val lblStatus: TextView
    private val progressCircle: ProgressCircleView

    init {
        LayoutInflater.from(context).inflate(R.layout.download_button, this, true)
        imgIcon = findViewById(R.id.imgIcon)
        lblStatus = findViewById(R.id.lblStatus)
        progressCircle = findViewById(R.id.progressCircle)
        orientation = LinearLayout.VERTICAL
        setRippleBackground(borderless = true)
    }

    var state: DownloadButton.State = State.NotDownloaded(context.getString(LR.string.podcasts_download_download))
        set(value) {
            when (value) {
                is State.NotDownloaded -> {
                    imgIcon.setImageResource(IR.drawable.ic_download)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(tintColor))
                    lblStatus.text = value.downloadSize
                    progressCircle.isVisible = false
                }
                is State.Queued -> {
                    imgIcon.setImageResource(IR.drawable.ic_stop)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(tintColor))
                    lblStatus.text = context.getString(LR.string.podcasts_download_queued)
                    progressCircle.isVisible = false
                }
                is State.Downloading -> {
                    lblStatus.text = "${round(value.progressPercent * 100f)}%"
                    imgIcon.setImageResource(IR.drawable.ic_downloading)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(tintColor))
                    progressCircle.setPercent(value.progressPercent)
                    progressCircle.isVisible = true
                }
                is State.Downloaded -> {
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(context.getThemeColor(UR.attr.support_02)))
                    lblStatus.text = value.downloadSize
                    imgIcon.setImageResource(IR.drawable.ic_downloaded)
                    progressCircle.isVisible = false
                }
                is State.Errored -> {
                    lblStatus.text = context.getString(LR.string.podcasts_download_retry)
                    imgIcon.setImageResource(R.drawable.ic_retry)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(tintColor))
                    progressCircle.isVisible = false
                }
            }
        }

    @ColorInt var tintColor: Int = 0
        set(value) {
            progressCircle.setColor(value)
            field = value
        }

    sealed class State {
        data class NotDownloaded(val downloadSize: String) : State()
        object Queued : State()
        data class Downloading(val progressPercent: Float) : State()
        data class Downloaded(val downloadSize: String) : State()
        object Errored : State()
    }
}
