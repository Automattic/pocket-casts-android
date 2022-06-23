package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground

class DiskSpaceSizeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var onCheckedChanged: ((Boolean) -> Unit)? = null
    val isChecked: Boolean
        get() = checkbox.isChecked

    private val checkbox: CheckBox
    private val lblTitle: TextView
    private val lblSubtitle: TextView
    private val bar: ProgressBar

    init {
        LayoutInflater.from(context).inflate(R.layout.view_diskspace, this, true)
        checkbox = findViewById(R.id.checkbox)
        lblTitle = findViewById(R.id.lblTitle)
        lblSubtitle = findViewById(R.id.lblSubtitle)
        bar = findViewById(R.id.bar)
        setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
        }

        checkbox.setOnCheckedChangeListener { _, isChecked -> onCheckedChanged?.invoke(isChecked) }
        setRippleBackground()
    }

    fun setup(@StringRes title: Int, percentage: Int, subtitle: String) {
        lblTitle.setText(title)
        lblSubtitle.text = subtitle
        bar.progress = percentage
        checkbox.isChecked = false
    }

    fun update(percentage: Int, subtitle: String) {
        lblSubtitle.text = subtitle
        bar.progress = percentage
    }
}
