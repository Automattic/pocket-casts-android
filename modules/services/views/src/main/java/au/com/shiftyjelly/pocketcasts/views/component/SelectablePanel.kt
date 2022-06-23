package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class SelectablePanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private var currentSelectColor: Int = context.getThemeColor(UR.attr.primary_interactive_01)
    private var currentDeselectColor: Int = context.getThemeColor(UR.attr.primary_ui_05)
    private var currentStrokeWidth: Int = 4

    override fun setSelected(active: Boolean) {
        val gradient = background as GradientDrawable
        gradient.setStroke(currentStrokeWidth, if (active) currentSelectColor else currentDeselectColor)
    }

    fun setSelectedWithColors(active: Boolean, selectedColor: Int, deselectedColor: Int, strokeWidth: Int) {
        currentSelectColor = selectedColor
        currentDeselectColor = deselectedColor
        currentStrokeWidth = strokeWidth
        isSelected = active
    }
}
