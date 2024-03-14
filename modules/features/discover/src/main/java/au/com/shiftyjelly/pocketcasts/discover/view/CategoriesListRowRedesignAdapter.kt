package au.com.shiftyjelly.pocketcasts.discover.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.CategoryPillBinding
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList

@SuppressLint("NotifyDataSetChanged")
class CategoriesListRowRedesignAdapter(
    private val onCategoryClick: (NetworkLoadableList) -> Unit,
    private val onAllCategoriesClick: (() -> Unit) -> Unit,
    private val categories: List<CategoryPill>,
) : RecyclerView.Adapter<CategoriesListRowRedesignAdapter.CategoriesRedesignViewHolder>() {
    companion object {
        const val ALL_CATEGORIES_INDEX = 0
    }

    private val currentCategories = categories.map { it.copy() }.toMutableList()

    class CategoriesRedesignViewHolder(
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
                binding.categoryName.visibility = View.GONE
                binding.categoryIcon.visibility = View.VISIBLE
                binding.categoryIcon.setImageResource(R.drawable.ic_arrow_close)
                binding.categoryIcon.contentDescription =
                    getString(context, au.com.shiftyjelly.pocketcasts.localization.R.string.clear_all)
                binding.categoryPill.background =
                    getDrawable(context, R.drawable.category_clear_all_pill_background)
            } else {
                binding.categoryName.visibility = View.VISIBLE
                binding.categoryIcon.visibility = View.VISIBLE
                binding.categoryIcon.setImageResource(R.drawable.ic_arrow_down)
                binding.categoryPill.background = getDrawable(context, R.drawable.category_pill_background)
                binding.categoryName.text = category.discoverCategory.name
                binding.categoryName.contentDescription = category.discoverCategory.name
            }
        }
        private fun setUpCategories(context: Context, category: CategoryPill) {
            if (category.isSelected) {
                binding.categoryPill.background = getDrawable(context, R.drawable.category_pill_selected_background)
                binding.categoryName.setTextColor(Color.WHITE)
            } else {
                binding.categoryPill.background = getDrawable(context, R.drawable.category_pill_background)
                binding.categoryName.setTextAppearance(au.com.shiftyjelly.pocketcasts.ui.R.style.H40)
            }
            binding.categoryName.text = category.discoverCategory.name
            binding.categoryName.contentDescription = category.discoverCategory.name
            binding.categoryName.visibility = View.VISIBLE
            binding.categoryIcon.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoriesRedesignViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CategoryPillBinding.inflate(inflater, parent, false)
        return CategoriesRedesignViewHolder(binding) { position ->
            val category = currentCategories[position]
            if (category.discoverCategory.id == DiscoverCategory.ALL_CATEGORIES_ID) {
                if (category.isSelected) {
                    clearCategoryFilter()
                } else {
                    binding.categoryIcon.setImageResource(R.drawable.ic_arrow_up)
                    onAllCategoriesClick onCategorySelectionCancel@{
                        binding.categoryIcon.setImageResource(R.drawable.ic_arrow_down)
                    }
                }
            } else {
                selectCategory(position)
                onCategoryClick(category.discoverCategory)
            }
        }
    }

    override fun onBindViewHolder(holder: CategoriesRedesignViewHolder, position: Int) {
        holder.bind(currentCategories[position], holder.itemView.context)
    }
    override fun getItemCount(): Int {
        return currentCategories.size
    }
    private fun selectCategory(position: Int) {
        val selectedItem = currentCategories[position].copy()
        val allCategories = currentCategories[ALL_CATEGORIES_INDEX].copy()

        selectedItem.isSelected = true
        allCategories.isSelected = true

        currentCategories.clear()
        currentCategories.add(allCategories)
        currentCategories.add(selectedItem)

        notifyDataSetChanged()
    }
    private fun clearCategoryFilter() {
        currentCategories.clear()
        currentCategories.addAll(categories)
        notifyDataSetChanged()
    }
}
