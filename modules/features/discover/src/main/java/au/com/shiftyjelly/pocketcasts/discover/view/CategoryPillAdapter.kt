package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.discover.databinding.AllCategoriesPillBinding
import au.com.shiftyjelly.pocketcasts.discover.databinding.CategoryPillBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class CategoryPillAdapter(
    private val onCategoryClick: (DiscoverCategory) -> Unit,
    private val onDismissClick: (DiscoverCategory) -> Unit,
    private val onAllCategoriesClick: () -> Unit,
) : ListAdapter<CategoryPill, RecyclerView.ViewHolder>(CategoryPillDiffCallback) {
    private var state = CategoriesManager.State.Empty

    init {
        setHasStableIds(true)
    }

    fun submitState(state: CategoriesManager.State) {
        submitList(state.toPills()) { this.state = state }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is CategoryPill.AllItems -> R.layout.all_categories_pill
        is CategoryPill.Category -> R.layout.category_pill
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.all_categories_pill -> AllCategoriesPillViewHolder(
                binding = AllCategoriesPillBinding.inflate(inflater, parent, false),
                onAllCategoriesClick = onAllCategoriesClick,
                onDismissClick = onDismissClick,
            )

            R.layout.category_pill -> CategoryPillViewHolder(
                binding = CategoryPillBinding.inflate(inflater, parent, false),
                onCategoryClick = onCategoryClick,
            )

            else -> error("Unknown category pill view type: ${runCatching { parent.resources.getResourceName(viewType) }.getOrDefault("$viewType")}")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AllCategoriesPillViewHolder -> holder.bind(getItem(position) as CategoryPill.AllItems)
            is CategoryPillViewHolder -> holder.bind(getItem(position) as CategoryPill.Category)
            else -> error("Unknown holder type: $holder")
        }
    }
}

sealed interface CategoryPill {
    val id: Int

    sealed interface AllItems : CategoryPill {
        override val id get() = Int.MIN_VALUE

        data object Collapsed : AllItems
        data object Expanded : AllItems
        data class Selected(val category: DiscoverCategory) : AllItems
    }

    data class Category(
        val value: DiscoverCategory,
        val isSelected: Boolean,
    ) : CategoryPill {
        override val id get() = value.id
    }
}

private class AllCategoriesPillViewHolder(
    private val binding: AllCategoriesPillBinding,
    private val onDismissClick: (DiscoverCategory) -> Unit,
    private val onAllCategoriesClick: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val context get() = itemView.context

    private lateinit var pill: CategoryPill.AllItems

    init {
        binding.categoryPill.setOnClickListener {
            val selectedCategory = when (val pill = pill) {
                is CategoryPill.AllItems.Selected -> pill.category
                is CategoryPill.AllItems.Collapsed -> null
                is CategoryPill.AllItems.Expanded -> null
            }
            if (selectedCategory != null) {
                onDismissClick(selectedCategory)
            } else {
                onAllCategoriesClick()
            }
        }
    }

    fun bind(pill: CategoryPill.AllItems) {
        this.pill = pill

        when (pill) {
            is CategoryPill.AllItems.Selected -> {
                binding.categoryName.text = ""
                binding.categoryPill.contentDescription = context.getString(LR.string.discover_dismiss_selected_category)
                binding.categoryIcon.setImageResource(R.drawable.ic_arrow_close)
                binding.categoryPill.background = ContextCompat.getDrawable(context, R.drawable.category_clear_all_pill_background)
            }
            is CategoryPill.AllItems.Collapsed -> {
                binding.categoryName.text = context.getString(LR.string.discover_all_categories)
                binding.categoryPill.contentDescription = context.getString(LR.string.discover_show_all_categories)
                binding.categoryIcon.setImageResource(R.drawable.ic_arrow_down)
                binding.categoryPill.background = ContextCompat.getDrawable(context, R.drawable.category_pill_background)
            }
            is CategoryPill.AllItems.Expanded -> {
                binding.categoryName.text = context.getString(LR.string.discover_all_categories)
                binding.categoryPill.contentDescription = context.getString(LR.string.discover_show_all_categories)
                binding.categoryIcon.setImageResource(R.drawable.ic_arrow_up)
                binding.categoryPill.background = ContextCompat.getDrawable(context, R.drawable.category_pill_background)
            }
        }
    }
}

private class CategoryPillViewHolder(
    private val binding: CategoryPillBinding,
    private val onCategoryClick: (DiscoverCategory) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val context get() = itemView.context

    private lateinit var pill: CategoryPill.Category

    init {
        binding.categoryPill.setOnClickListener {
            onCategoryClick(pill.value)
        }
    }

    fun bind(pill: CategoryPill.Category) {
        this.pill = pill

        binding.categoryName.text = pill.value.name.tryToLocalise(context.resources)
        if (pill.isSelected) {
            binding.categoryName.setTextColor(context.getThemeColor(UR.attr.secondary_ui_01))
            binding.categoryPill.background = ContextCompat.getDrawable(context, R.drawable.category_pill_selected_background)
        } else {
            binding.categoryName.setTextColor(context.getThemeColor(UR.attr.primary_text_01))
            binding.categoryPill.background = ContextCompat.getDrawable(context, R.drawable.category_pill_background)
        }
    }
}

private fun CategoriesManager.State.toPills() = when (this) {
    is CategoriesManager.State.Idle -> toPills()
    is CategoriesManager.State.Selected -> toPills()
}

private fun CategoriesManager.State.Idle.toPills(): List<CategoryPill> {
    val categories = featuredCategories.map { category -> CategoryPill.Category(category, isSelected = false) }
    return if (categories.isNotEmpty()) {
        buildList {
            if (areAllCategoriesShown) {
                add(CategoryPill.AllItems.Expanded)
            } else {
                add(CategoryPill.AllItems.Collapsed)
            }
            addAll(categories)
        }
    } else {
        emptyList()
    }
}

private fun CategoriesManager.State.Selected.toPills() = buildList {
    add(CategoryPill.AllItems.Selected(selectedCategory))
    add(CategoryPill.Category(selectedCategory, isSelected = true))
}

private object CategoryPillDiffCallback : DiffUtil.ItemCallback<CategoryPill>() {
    override fun areItemsTheSame(oldItem: CategoryPill, newItem: CategoryPill): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CategoryPill, newItem: CategoryPill): Boolean {
        return oldItem == newItem
    }
}
