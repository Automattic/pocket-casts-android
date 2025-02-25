package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCategoryBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import coil.load

class CategoriesBottomSheetAdapter(
    private val onCategoryClick: (DiscoverCategory) -> Unit,
) : ListAdapter<DiscoverCategory, CategoriesBottomSheetAdapter.ViewHolder>(CategoryDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryBinding.inflate(inflater, parent, false)

        return ViewHolder(binding, onCategoryClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCategoryBinding,
        onItemClicked: (DiscoverCategory) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var category: DiscoverCategory

        init {
            binding.itemCategoryLinear.setOnClickListener {
                onItemClicked(category)
            }
        }

        fun bind(category: DiscoverCategory) {
            this.category = category

            val localisedName = category.name.tryToLocalise(itemView.context.resources)
            binding.lblTitle.text = localisedName
            binding.lblTitle.contentDescription = localisedName
            binding.imageView.load(category.icon)
        }
    }
}

private object CategoryDiffCallback : DiffUtil.ItemCallback<DiscoverCategory>() {
    override fun areItemsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem == newItem
    }
}
