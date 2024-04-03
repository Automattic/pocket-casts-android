package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.discover.R.layout
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

class CategoriesBottomSheet(
    private val categories: List<CategoryPill>,
    private val onCategoryClick: (CategoryPill) -> Unit,
    private val onCategorySelectionCancel: () -> Unit,
) : BaseDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(layout.fragment_categories_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView: RecyclerView =
            view.findViewById(au.com.shiftyjelly.pocketcasts.discover.R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        val adapter = CategoriesBottomSheetAdapter {
            onCategoryClick(it)
            dismiss()
        }
        recyclerView.adapter = adapter

        val sortedCategories = categories.map {
            it.copy(discoverCategory = it.discoverCategory.copy(name = it.discoverCategory.name.tryToLocalise(resources)))
        }.sortedBy { it.discoverCategory.name }

        adapter.submitList(sortedCategories)

        val behavior = BottomSheetBehavior.from(view.parent as View)
        val windowHeight = resources.displayMetrics.heightPixels
        val halfHeight = windowHeight / 2
        behavior.peekHeight = halfHeight
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
    override fun onCancel(dialog: DialogInterface) {
        this.onCategorySelectionCancel.invoke()
    }
}
