package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.views.extensions.setSystemWindowInsetToPadding
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CategoriesBottomSheet : BaseDialogFragment() {
    @Inject lateinit var categoriesManager: CategoriesManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    private val region get() = requireArguments().getString(RegionKey, "")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_categories_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.categoriesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        val adapter = CategoriesBottomSheetAdapter { category ->
            trackCategorySelected(category.name, category.id)
            categoriesManager.selectCategory(category.id)
            dismiss()
        }
        recyclerView.adapter = adapter
        recyclerView.setSystemWindowInsetToPadding(bottom = true)

        val sortedCategories = categoriesManager.state.value
            .allCategories
            .sortedBy { it.name.tryToLocalise(resources) }
        adapter.submitList(sortedCategories)

        val behavior = BottomSheetBehavior.from(view.parent as View)
        val windowHeight = resources.displayMetrics.heightPixels
        val halfHeight = windowHeight / 2
        behavior.peekHeight = halfHeight
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        categoriesManager.setAllCategoriesShown(true)
        trackShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        categoriesManager.setAllCategoriesShown(false)
        trackDismissed()
    }

    private fun trackCategorySelected(name: String, id: Int) {
        analyticsTracker.track(
            AnalyticsEvent.DISCOVER_CATEGORIES_PICKER_PICK,
            mapOf("name" to name, "id" to id, "region" to region),
        )
    }

    private fun trackShown() {
        analyticsTracker.track(
            AnalyticsEvent.DISCOVER_CATEGORIES_PICKER_SHOWN,
            mapOf("region" to region),
        )
    }

    private fun trackDismissed() {
        analyticsTracker.track(
            AnalyticsEvent.DISCOVER_CATEGORIES_PICKER_CLOSED,
            mapOf("region" to region),
        )
    }

    companion object {
        private const val RegionKey = "Region"

        fun newInstance(region: String) = CategoriesBottomSheet().apply {
            arguments = bundleOf(RegionKey to region)
        }
    }
}
