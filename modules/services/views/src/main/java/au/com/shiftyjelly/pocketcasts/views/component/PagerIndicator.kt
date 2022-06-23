package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.R

class PagerIndicator @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private val dotWidth = 14.dpToPx(context)
    private val dotHeight = 8.dpToPx(context)

    var count: Int = 0
        set(value) {
            field = value
            buildDots()
        }

    var position: Int = 0
        set(value) {
            field = value
            updateSelected()
        }

    init {
        orientation = HORIZONTAL
    }

    private fun addDot() {
        val image = ImageView(context).apply {
            layoutParams = LayoutParams(dotWidth, dotHeight)
            background = AppCompatResources.getDrawable(context, R.drawable.view_pager_indicator)
        }

        addView(image, childCount)
    }

    private fun buildDots() {
        removeAllViews()
        repeat(count) {
            addDot()
        }
        updateSelected()
    }

    private fun updateSelected() {
        children.forEachIndexed { index, view ->
            view.isSelected = position == index
        }
    }
}
