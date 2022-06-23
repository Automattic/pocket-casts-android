package au.com.shiftyjelly.pocketcasts.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.settings.databinding.AdapterSettingsBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import java.util.UUID

private class SettingsDiff : DiffUtil.ItemCallback<SettingsAdapter.Item>() {
    override fun areItemsTheSame(oldItem: SettingsAdapter.Item, newItem: SettingsAdapter.Item): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SettingsAdapter.Item, newItem: SettingsAdapter.Item): Boolean {
        return oldItem == newItem
    }
}

class SettingsAdapter(
    private val sections: List<Item>,
    private val clickListener: (Item) -> Unit
) : ListAdapter<SettingsAdapter.Item, SettingsAdapter.ViewHolder>(SettingsDiff()) {

    init {
        setHasStableIds(true)

        submitList(sections)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterSettingsBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = getItem(position)
        holder.bind(section)
    }

    override fun getItemCount() = sections.size

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    inner class ViewHolder(val binding: AdapterSettingsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: Item) {
            val context = itemView.context
            val startColor = context.getThemeColor(section.gradientStartAttr)
            val endColor = context.getThemeColor(section.gradientEndAttr)
            val drawable = AppCompatResources.getDrawable(context, section.icon)

            section.show?.let { itemView.showIf(it()) }

            val titleView = binding.titleView
            val imageView = binding.imageView
            val rowView = binding.rowView

            val title = context.getString(section.title)
            titleView.text = title
            imageView.setup(drawable, startColor, endColor)
            rowView.contentDescription = title

            val endDrawable = if (section.plusSection) AppCompatResources.getDrawable(context, R.drawable.ic_plus) else null
            titleView.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
            titleView.compoundDrawablePadding = 8.dpToPx(context)

            rowView.setOnClickListener { clickListener(section) }
        }
    }

    data class Item(
        @StringRes val title: Int,
        @DrawableRes val icon: Int,
        val fragment: Class<out Fragment>? = null,
        val show: (() -> Boolean)? = null,
        val action: (() -> Unit)? = null,
        val gradientStartAttr: Int = au.com.shiftyjelly.pocketcasts.ui.R.attr.primary_interactive_01,
        val gradientEndAttr: Int = au.com.shiftyjelly.pocketcasts.ui.R.attr.primary_interactive_01,
        var plusSection: Boolean = false,
        val id: Long = UUID.randomUUID().hashCode().toLong()
    )
}
