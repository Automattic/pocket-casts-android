package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.graphics.Color.WHITE
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.ContextCompat.getString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.CategoryPillBinding
import au.com.shiftyjelly.pocketcasts.discover.view.CategoryPillListAdapter.CategoryPillViewHolder
import au.com.shiftyjelly.pocketcasts.localization.R.string.clear_all
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory

val CATEGORY_PILL_DIFF = object : DiffUtil.ItemCallback<CategoryPill>() {
    override fun areItemsTheSame(oldItem: CategoryPill, newItem: CategoryPill): Boolean =
        oldItem.discoverCategory.id == newItem.discoverCategory.id

    override fun areContentsTheSame(oldItem: CategoryPill, newItem: CategoryPill): Boolean =
        oldItem == newItem
}

class CategoryPillListAdapter(
    private val onCategoryClick: (CategoryPill, (List<CategoryPill>) -> Unit) -> Unit,
    private val onAllCategoriesClick: (() -> Unit, (List<CategoryPill>) -> Unit) -> Unit,
    private val onClearCategoryClick: () -> Unit,
) : ListAdapter<CategoryPill, CategoryPillViewHolder>(CATEGORY_PILL_DIFF) {

    class CategoryPillViewHolder(
        val binding: CategoryPillBinding,
        private val onItemClicked: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.categoryPill.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
            }
        }

        fun bind(category: CategoryPill, context: Context) {
            if (category.discoverCategory.id == DiscoverCategory.ALL_CATEGORIES_ID) {
                setUpAllCategoriesAndClear(context, category)
            } else {
                setUpCategories(context, category)
            }
        }

        private fun setUpAllCategoriesAndClear(context: Context, category: CategoryPill) {
            if (category.isSelected) {
                binding.categoryName.isVisible = false
                binding.categoryIcon.setIcon(R.drawable.ic_arrow_close, getString(context, clear_all))
                binding.categoryPill.background = getDrawable(context, R.drawable.category_clear_all_pill_background)
            } else {
                binding.categoryName.setCategory(category.discoverCategory.name)
                binding.categoryIcon.setIcon(R.drawable.ic_arrow_down)
                binding.categoryPill.background = getDrawable(context, R.drawable.category_pill_background)
            }
        }

        private fun setUpCategories(context: Context, category: CategoryPill) {
            if (category.isSelected) {
                binding.categoryPill.background =
                    getDrawable(context, R.drawable.category_pill_selected_background)
                binding.categoryName.setTextColor(WHITE)
            } else {
                binding.categoryPill.background = getDrawable(context, R.drawable.category_pill_background)
                binding.categoryName.setTextAppearance(au.com.shiftyjelly.pocketcasts.ui.R.style.H40)
            }
            binding.categoryName.setCategory(category.discoverCategory.name)
            binding.categoryIcon.isVisible = false
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoryPillViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CategoryPillBinding.inflate(inflater, parent, false)
        return CategoryPillViewHolder(binding) { position ->
            val category = getItem(position)
            if (category.discoverCategory.id == DiscoverCategory.ALL_CATEGORIES_ID) {
                if (category.isSelected) {
                    onClearCategoryClick()
                } else {
                    binding.categoryIcon.setImageResource(R.drawable.ic_arrow_up)
                    onAllCategoriesClick(
                        onCategorySelectionCancel@{
                            binding.categoryName.setCategory(category.discoverCategory.name)
                            binding.categoryIcon.setIcon(R.drawable.ic_arrow_down)
                        },
                        onCategorySelectionSuccess@{
                            binding.categoryIcon.setIcon(R.drawable.ic_arrow_down)
                            updateCategoryStatus(position, isSelected = false)
                            loadCategories(it)
                        },
                    )
                }
            } else if (!category.isSelected) {
                updateCategoryStatus(position, isSelected = true)
                onCategoryClick(category) {
                    loadCategories(it)
                }
            }
        }
    }
    override fun onBindViewHolder(holder: CategoryPillViewHolder, position: Int) {
        holder.bind(getItem(position), holder.itemView.context)
    }
    fun loadCategories(categoryPills: List<CategoryPill>) {
        submitList(categoryPills)
    }
    private fun updateCategoryStatus(position: Int, isSelected: Boolean) {
        getItem(position).isSelected = isSelected
        notifyItemChanged(position)
    }
}
private fun TextView.setCategory(category: String) {
    isVisible = true
    text = category
    contentDescription = category
}

private fun ImageView.setIcon(resourceId: Int, contentDescription: CharSequence? = null) {
    setImageResource(resourceId)
    isVisible = true
    contentDescription?.let { this.contentDescription = it }
}
