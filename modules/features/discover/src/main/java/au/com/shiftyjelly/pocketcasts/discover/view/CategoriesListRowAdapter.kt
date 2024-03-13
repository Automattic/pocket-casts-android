package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCategoryBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCategoryRedesignBinding
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory.Companion.ALL_CATEGORIES_ID
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import coil.load

val CATEGORY_DIFF = object : DiffUtil.ItemCallback<DiscoverCategory>() {
    override fun areItemsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: DiscoverCategory, newItem: DiscoverCategory): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}

class CategoriesListRowAdapter(val onPodcastListClick: (NetworkLoadableList) -> Unit) : ListAdapter<DiscoverCategory, CategoriesListRowAdapter.CategoryViewHolder>(CATEGORY_DIFF) {
    class CategoryViewHolder(
        val binding: ItemCategoryBinding,
        onItemClicked: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemCategoryLinear.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
            }
        }
        fun bind(category: DiscoverCategory) {
            binding.lblTitle.text = category.name
            binding.imageView.load(category.icon)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryBinding.inflate(inflater, parent, false)

        return CategoryViewHolder(binding) { position ->
            onPodcastListClick(getItem(position))
        }
    }
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
class CategoriesListRowRedesignAdapter(
    val onPodcastListClick: (NetworkLoadableList) -> Unit,
    val onAllCategoriesClick: (View) -> Unit,
) : ListAdapter<DiscoverCategory, CategoriesListRowRedesignAdapter.CategoriesRedesignViewHolder>(CATEGORY_DIFF) {

    class CategoriesRedesignViewHolder(
        val binding: ItemCategoryRedesignBinding,
        onItemClicked: (Int, View) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.categoryChip.setOnClickListener { view ->
                onItemClicked(bindingAdapterPosition, view)
            }
        }

        fun bind(category: DiscoverCategory) {
            binding.categoryChip.text = category.name
            binding.categoryChip.contentDescription = category.name
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoriesRedesignViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryRedesignBinding.inflate(inflater, parent, false)

        return CategoriesRedesignViewHolder(binding) { position, view ->
            val category = getItem(position)
            if (category.id == ALL_CATEGORIES_ID) {
                onAllCategoriesClick(view)
            } else {
                onPodcastListClick(category)
            }
        }
    }

    override fun onBindViewHolder(holder: CategoriesRedesignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
