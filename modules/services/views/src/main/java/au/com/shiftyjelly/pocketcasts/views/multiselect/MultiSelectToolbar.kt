package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class MultiSelectToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle
) : Toolbar(context, attrs, defStyleAttr) {

    companion object {
        const val MAX_ICONS = 4
    }

    private var overflowItems: List<MultiSelectAction> = emptyList()
    private var fragmentManager: FragmentManager? = null
    private var multiSelectHelper: MultiSelectHelper? = null
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    fun setup(
        lifecycleOwner: LifecycleOwner,
        multiSelectHelper: MultiSelectHelper,
        @MenuRes menuRes: Int?,
        fragmentManager: FragmentManager
    ) {

        this.fragmentManager = fragmentManager
        this.multiSelectHelper = multiSelectHelper

        setBackgroundColor(context.getThemeColor(UR.attr.support_01))
        if (menuRes != null) {
            inflateMenu(menuRes)
        } else {
            multiSelectHelper.toolbarActions.observe(lifecycleOwner) {
                menu.clear()

                it.subList(0, MAX_ICONS).forEachIndexed { _, action ->
                    val item = menu.add(Menu.NONE, action.actionId, 0, action.title)
                    item.setIcon(action.iconRes)
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }

                overflowItems = it.subList(MAX_ICONS, it.size)

                val overflow = menu.add(Menu.NONE, R.id.menu_overflow, 0, context.getString(LR.string.more_options))
                overflow.setIcon(IR.drawable.ic_more_vert_black_24dp)
                overflow.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.tintIcons(context.getThemeColor(UR.attr.primary_interactive_02))
            }
        }

        menu.tintIcons(context.getThemeColor(UR.attr.primary_interactive_02))

        setTitleTextColor(context.getThemeColor(UR.attr.primary_interactive_02))
        val overflowIcon = getOverflowIcon()!!
        val tintedOverflow = DrawableCompat.wrap(overflowIcon)
        DrawableCompat.setTint(tintedOverflow.mutate(), context.getThemeColor(UR.attr.primary_interactive_02))
        setOverflowIcon(tintedOverflow)

        setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_overflow) {
                analyticsTracker.track(AnalyticsEvent.MULTI_SELECT_VIEW_OVERFLOW_MENU_SHOWN, AnalyticsProp.sourceMap(multiSelectHelper.source))
                showOverflowBottomSheet()
                true
            } else {
                multiSelectHelper.onMenuItemSelected(itemId = it.itemId, resources = resources, fragmentManager = fragmentManager)
            }
        }

        multiSelectHelper.selectedCount.observe(lifecycleOwner) { count ->
            title = count.toString()
        }

        setNavigationOnClickListener {
            multiSelectHelper.isMultiSelecting = false
        }
        navigationContentDescription = context.getString(LR.string.back)
    }

    fun showOverflowBottomSheet() {
        val fragmentManager = fragmentManager ?: return
        val overflowSheet = MultiSelectBottomSheet.newInstance(overflowItems.map { it.actionId })
        overflowSheet.multiSelectHelper = multiSelectHelper
        overflowSheet.show(fragmentManager, "multiselectbottomsheet")
    }

    override fun setNavigationIcon(icon: Drawable?) {
        if (icon == null) {
            super.setNavigationIcon(icon)
            return
        }

        val tintedIcon = DrawableCompat.wrap(icon)
        DrawableCompat.setTint(tintedIcon.mutate(), context.getThemeColor(UR.attr.primary_interactive_02))
        super.setNavigationIcon(tintedIcon)
    }

    private object AnalyticsProp {
        private const val source = "source"

        fun sourceMap(eventSource: AnalyticsSource) =
            mapOf(source to eventSource.analyticsValue)
    }
}
