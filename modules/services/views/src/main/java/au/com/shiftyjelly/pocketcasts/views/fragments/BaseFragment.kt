package au.com.shiftyjelly.pocketcasts.views.fragments

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.None
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class BaseFragment : Fragment(), CoroutineScope, HasBackstack {

    open var statusBarColor: StatusBarColor = StatusBarColor.Light

    @Inject lateinit var theme: Theme

    @Inject lateinit var chromeCastAnalytics: ChromeCastAnalytics

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.background == null) {
            val backgroundColor = if (FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)) UR.attr.primary_ui_01 else UR.attr.primary_ui_04
            view.setBackgroundColor(view.context.getThemeColor(backgroundColor))
        }
        view.isClickable = true
        view.isFocusable = true
    }

    override fun onResume() {
        super.onResume()

        // Need to make sure we are the top fragment before updating the status bar
        val fragmentManager = activity?.supportFragmentManager
        if (fragmentManager != null && (fragmentManager.backStackEntryCount == 0 || fragmentManager.fragments.last() == this)) {
            updateStatusBar()
        }
    }

    fun updateStatusBar() {
        val activity = activity ?: return

        if (activity is FragmentHostListener) {
            activity.updateStatusBar()
        } else {
            theme.updateWindowStatusBar(window = activity.window, statusBarColor = statusBarColor, context = activity)
        }
    }

    fun updateStatusBarColor(@ColorInt color: Int) {
        statusBarColor = StatusBarColor.Custom(color = color, isWhiteIcons = theme.activeTheme.defaultLightIcons)
        updateStatusBar()
    }

    fun setupToolbarAndStatusBar(
        toolbar: Toolbar,
        title: String? = null,
        @MenuRes menu: Int? = null,
        chromeCastButton: ChromeCastButton = ChromeCastButton.None,
        navigationIcon: NavigationIcon = None,
        onNavigationClick: (() -> Unit)? = null,
        toolbarColors: ToolbarColors? = ToolbarColors.theme(theme = theme, context = toolbar.context),
    ) {
        toolbar.setup(
            title = title,
            menu = menu,
            chromeCastButton = chromeCastButton,
            navigationIcon = navigationIcon,
            onNavigationClick = onNavigationClick,
            activity = activity,
            theme = theme,
            toolbarColors = toolbarColors,
        )
        if (toolbarColors != null) {
            updateStatusBarColor(toolbarColors.backgroundColor)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        context?.let {
            menu.tintIcons(it.getThemeColor(UR.attr.secondary_icon_01))
        }
    }

    override fun onBackPressed(): Boolean {
        val childrenWithBackstack: List<HasBackstack> = childFragmentManager.fragments.filterIsInstance<HasBackstack>()
        // Some fragments have child fragments that require a back stack, we need to check for those
        // before popping the main back stack
        if (childrenWithBackstack.count() > 0) {
            var handled = false
            var index = 0
            do {
                val child = childrenWithBackstack[index++]
                if (child is Fragment) {
                    handled = child.onBackPressed()
                }
            } while (!handled && index < childrenWithBackstack.count())
            if (handled) {
                return true
            }
        }

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStackImmediate()
            return true
        }

        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }
}
