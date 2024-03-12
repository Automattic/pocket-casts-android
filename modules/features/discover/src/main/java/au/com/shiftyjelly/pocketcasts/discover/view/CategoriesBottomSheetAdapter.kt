package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import coil.load

class CategoriesBottomSheetAdapter(val onCategoryClick: (NetworkLoadableList) -> Unit) : ListAdapter<DiscoverCategory, CategoriesBottomSheetAdapter.CategoryViewHolder>(CATEGORY_DIFF) {
    class CategoryViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: ImageView = itemView.findViewById(R.id.imageView)
        val categoryName: TextView = itemView.findViewById(R.id.lblTitle)
        val itemCategoryLayout: LinearLayout = itemView.findViewById(R.id.itemCategoryLinear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.categoryName.text = category.name
        holder.categoryName.contentDescription = category.name
        holder.categoryIcon.load(category.icon)
        holder.itemCategoryLayout.setOnClickListener { onCategoryClick(category) }
    }
}
