package au.com.shiftyjelly.pocketcasts.filters

import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView

data class FilterOption(
    @StringRes val title: Int,
    var isChecked: Boolean,
    val onCheckedChange: (Boolean, Int) -> Unit,
    val playlistValue: Int? = null // Value that this gets stored as in the playlist db
)

sealed class FilterOptionsAdapterType {
    object Checkbox : FilterOptionsAdapterType()
    object Radio : FilterOptionsAdapterType()
}

class FilterOptionsAdapter(val options: List<FilterOption>, val adapterType: FilterOptionsAdapterType = FilterOptionsAdapterType.Checkbox, @ColorInt val tintColor: Int) : RecyclerView.Adapter<FilterOptionRowViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterOptionRowViewHolder {
        return FilterOptionRowViewHolder(adapterType, parent)
    }

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: FilterOptionRowViewHolder, position: Int) {
        holder.bind(options[position], position, tintColor)
    }
}
