package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCategoryBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import coil.load

private val CATEGORY_DIFF = object : DiffUtil.ItemCallback<DiscoverCategory>() {
    override fun areItemsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}

class CategoriesListRowAdapter(val onPodcastListClick: (NetworkLoadableList) -> Unit) : ListAdapter<DiscoverCategory, CategoriesListRowAdapter.CategoryViewHolder>(CATEGORY_DIFF) {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryBinding.inflate(inflater, parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.binding.lblTitle.text = category.name.tryToLocalise(holder.itemView.resources)
        holder.binding.imageView.load(category.icon)
        holder.itemView.setOnClickListener { onPodcastListClick(category) }
    }
}
