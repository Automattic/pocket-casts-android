package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

private fun constructItemView(adapterType: FilterOptionsAdapterType, parent: ViewGroup): View {
    val layoutId = if (adapterType is FilterOptionsAdapterType.Checkbox) R.layout.filters_checkbox_row else R.layout.filters_radio_row
    return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
}

class FilterOptionRowViewHolder(adapterType: FilterOptionsAdapterType, parent: ViewGroup) : RecyclerView.ViewHolder(constructItemView(adapterType, parent)) {
    private val checkbox = itemView.findViewById<CompoundButton>(R.id.checkbox)
    private val lblTitle = itemView.findViewById<TextView>(R.id.lblTitle)

    init {
        itemView.setOnClickListener {
            if (checkbox is RadioButton) {
                if (!checkbox.isChecked) {
                    checkbox.isChecked = true
                }
            } else {
                checkbox.isChecked = !checkbox.isChecked
            }
        }
    }

    fun bind(filterOption: FilterOption, position: Int, @ColorInt tintColor: Int) {
        lblTitle.setText(filterOption.title)
        checkbox.isChecked = filterOption.isChecked
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            filterOption.onCheckedChange(isChecked, position)
        }
        checkbox.buttonTintList = ColorStateList.valueOf(tintColor)
    }
}
