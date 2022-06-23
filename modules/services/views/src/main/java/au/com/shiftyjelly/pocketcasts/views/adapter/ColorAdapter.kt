package au.com.shiftyjelly.pocketcasts.views.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class ColorAdapter(val colorList: IntArray, val readOnly: Boolean, val onSelectedChange: (Int) -> Unit) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var currentReadOnly = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        currentReadOnly = readOnly
    }

    var selectedIndex = 0
        set(value) {
            field = value
            onSelectedChange(value)
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = IconView(parent.context)
        val layoutParams = RecyclerView.LayoutParams(44.dpToPx(parent.context), 44.dpToPx(parent.context))
        layoutParams.marginEnd = 16.dpToPx(parent.context)
        view.layoutParams = layoutParams
        view.setBackgroundResource(R.drawable.filter_circle)
        view.imageTintList = ColorStateList.valueOf(view.context.getThemeColor(UR.attr.primary_interactive_02))
        view.isClickable = true
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        val padding = 12.dpToPx(view.context)
        view.setPadding(padding, padding, padding, padding)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return colorList.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colorList[position]
        val iconView = holder.itemView as IconView

        iconView.backgroundTintList = ColorStateList.valueOf(color)
        setupView(iconView, selectedIndex == position)
        iconView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                notifyItemChanged(selectedIndex)
                selectedIndex = position
            }

            setupView(iconView, selectedIndex == position)
        }
    }

    private fun setupView(iconView: IconView, selected: Boolean) {
        if (currentReadOnly) {
            iconView.setImageResource(IR.drawable.ic_locked)
        } else if (selected) {
            iconView.setImageResource(IR.drawable.ic_tick_small)
        } else {
            iconView.setImageDrawable(null)
        }
    }
}

class IconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        val padding = 7.dpToPx(context)
        setPadding(padding, padding, padding, padding)
    }
}
