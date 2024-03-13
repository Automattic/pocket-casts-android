package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.databinding.ItemCategoryBinding
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import coil.load

class CategoriesBottomSheetAdapter(
    private val onCategoryClick: (NetworkLoadableList) -> Unit,
) : ListAdapter<DiscoverCategory, CategoriesBottomSheetAdapter.CategoryViewHolder>(CATEGORY_DIFF) {
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
            binding.lblTitle.contentDescription = category.name
            binding.imageView.load(category.icon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryBinding.inflate(inflater, parent, false)

        return CategoryViewHolder(binding) { position ->
            onCategoryClick(getItem(position))
        }
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
