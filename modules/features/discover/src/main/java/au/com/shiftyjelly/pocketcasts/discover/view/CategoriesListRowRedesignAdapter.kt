package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.CategoryPillBinding
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList

private val CATEGORY_REDESIGN_DIFF = object : DiffUtil.ItemCallback<CategoryPillRow>() {
    override fun areItemsTheSame(oldItem: CategoryPillRow, newItem: CategoryPillRow): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: CategoryPillRow, newItem: CategoryPillRow): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}

class CategoriesListRowRedesignAdapter(
    val onPodcastListClick: (NetworkLoadableList) -> Unit,
    val onAllCategoriesClick: (View) -> Unit,
) : ListAdapter<CategoryPillRow, CategoriesListRowRedesignAdapter.CategoriesRedesignViewHolder>(CATEGORY_REDESIGN_DIFF) {

    class CategoriesRedesignViewHolder(
        val binding: CategoryPillBinding,
        onItemClicked: (Int, View) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.categoryName.setOnClickListener { view ->
                onItemClicked(bindingAdapterPosition, view)
            }
        }

        fun bind(category: CategoryPillRow) {
            binding.categoryName.text = category.discoverCategory.name
            binding.categoryName.contentDescription = category.discoverCategory.name
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoriesRedesignViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CategoryPillBinding.inflate(inflater, parent, false)

        return CategoriesRedesignViewHolder(binding) { position, view ->
            val category = getItem(position)
            if (category.discoverCategory.id == DiscoverCategory.ALL_CATEGORIES_ID) {
                onAllCategoriesClick(view)
            } else {
                onPodcastListClick(category.discoverCategory)
            }
        }
    }

    override fun onBindViewHolder(holder: CategoriesRedesignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
