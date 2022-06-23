package au.com.shiftyjelly.pocketcasts.views.tour

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import au.com.shiftyjelly.pocketcasts.views.R
import com.google.android.material.button.MaterialButton

class TourStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val view = inflater.inflate(R.layout.tour_step_view, this)
    private val lblHeading = view.findViewById<TextView>(R.id.lblHeading)
    private val lblBody = view.findViewById<TextView>(R.id.lblBody)
    private val btnNext = view.findViewById<MaterialButton>(R.id.btnNext)
    private val lblStep = view.findViewById<TextView>(R.id.lblStep)
    private val btnEnd = view.findViewById<MaterialButton>(R.id.btnEnd)

    init {
        setBackgroundResource(R.drawable.tour_step_background)
    }

    fun setup(step: TourStep, buttonPressed: () -> Unit) {
        lblHeading.text = step.title
        lblBody.text = step.description
        btnNext.text = step.buttonText
        btnNext.setOnClickListener { buttonPressed() }
    }

    fun setupStepText(stepText: String, @ColorInt color: Int) {
        lblStep.text = stepText
        lblStep.setTextColor(color)
    }

    fun setupCloseButton(text: String, closePressed: () -> Unit) {
        btnEnd.text = text
        btnEnd.setOnClickListener { closePressed() }
    }
}
