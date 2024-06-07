package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class MultiSelectToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle,
) : Toolbar(context, attrs, defStyleAttr) {

    private var overflowItems: List<MultiSelectAction> = emptyList()

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    fun <T> setup(
        lifecycleOwner: LifecycleOwner,
        multiSelectHelper: MultiSelectHelper<T>,
        @MenuRes menuRes: Int?,
        activity: FragmentActivity,
    ) {
        setBackgroundColor(context.getThemeColor(UR.attr.support_01))
        if (menuRes != null) {
            inflateMenu(menuRes)
        } else {
            multiSelectHelper.toolbarActions.removeObservers(lifecycleOwner)
            multiSelectHelper.toolbarActions.observe(lifecycleOwner) {
                Timber.d("MultiSelectToolbar setup observed toolbarActionChange,$it from ${multiSelectHelper.source}")

                menu.clear()

                val maxIcons = multiSelectHelper.maxToolbarIcons
                it.subList(0, maxIcons).forEachIndexed { _, action ->
                    val item = menu.add(Menu.NONE, action.actionId, 0, action.title)
                    item.setIcon(action.iconRes)
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    item.isVisible = action.isVisible
                }

                overflowItems = it.subList(maxIcons, it.size)

                when (multiSelectHelper) {
                    is MultiSelectBookmarksHelper -> {
                        overflowItems.forEachIndexed { _, action ->
                            val item = menu.add(Menu.NONE, action.actionId, 0, action.title)
                            item.setIcon(action.iconRes)
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                        }
                    }
                    is MultiSelectEpisodesHelper -> {
                        val overflow = menu.add(Menu.NONE, R.id.menu_overflow, 0, context.getString(LR.string.more_options))
                        overflow.setIcon(IR.drawable.ic_more_vert_black_24dp)
                        overflow.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    }
                }

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
                if (multiSelectHelper is MultiSelectEpisodesHelper) {
                    analyticsTracker.track(
                        AnalyticsEvent.MULTI_SELECT_VIEW_OVERFLOW_MENU_SHOWN,
                        AnalyticsProp.sourceMap(multiSelectHelper.source),
                    )
                    showOverflowBottomSheet(activity.supportFragmentManager, multiSelectHelper)
                }
                true
            } else {
                multiSelectHelper.onMenuItemSelected(itemId = it.itemId, resources = resources, activity = activity)
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

    private fun showOverflowBottomSheet(
        fragmentManager: FragmentManager?,
        multiSelectHelper: MultiSelectEpisodesHelper,
    ) {
        if (fragmentManager == null) return
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

        fun sourceMap(eventSource: SourceView) =
            mapOf(source to eventSource.analyticsValue)
    }
}
