package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.remember
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.DiscoverCategoriesPickerClosedEvent
import com.automattic.eventhorizon.DiscoverCategoriesPickerPickEvent
import com.automattic.eventhorizon.DiscoverCategoriesPickerShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CategoriesBottomSheet : BaseDialogFragment() {
    @Inject lateinit var categoriesManager: CategoriesManager

    @Inject lateinit var eventHorizon: EventHorizon

    private val region get() = requireArguments().getString(REGION_KEY, "")

    override val includeNavigationBarPadding = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            CategoriesBottomSheetPage(
                categories = remember {
                    categoriesManager.state.value
                        .allCategories
                        .sortedBy { it.name.tryToLocalise(resources) }
                },
                contentPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom).asPaddingValues(),
                onCategoryClick = ::onCategoryClick,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val behavior = BottomSheetBehavior.from(view.parent as View)
        val windowHeight = resources.displayMetrics.heightPixels
        val halfHeight = windowHeight / 2
        behavior.peekHeight = halfHeight
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        categoriesManager.setAllCategoriesShown(true)
        trackShown()
    }

    private fun onCategoryClick(category: DiscoverCategory) {
        trackCategorySelected(category)
        categoriesManager.selectCategory(category.id)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        categoriesManager.setAllCategoriesShown(false)
        trackDismissed()
    }

    private fun trackCategorySelected(category: DiscoverCategory) {
        eventHorizon.track(
            DiscoverCategoriesPickerPickEvent(
                name = category.name,
                region = region,
                id = category.id.toLong(),
                visits = category.totalVisits.toLong(),
                sponsored = category.isSponsored == true,
            ),
        )
    }

    private fun trackShown() {
        eventHorizon.track(
            DiscoverCategoriesPickerShownEvent(
                region = region,
            ),
        )
    }

    private fun trackDismissed() {
        eventHorizon.track(
            DiscoverCategoriesPickerClosedEvent(
                region = region,
            ),
        )
    }

    companion object {
        private const val REGION_KEY = "Region"

        fun newInstance(region: String) = CategoriesBottomSheet().apply {
            arguments = Bundle().apply {
                putString(REGION_KEY, region)
            }
        }
    }
}
