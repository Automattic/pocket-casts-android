package au.com.shiftyjelly.pocketcasts.views.fragments

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.extensions.updateProfileMenuBadge
import au.com.shiftyjelly.pocketcasts.views.extensions.updateProfileMenuImage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ProfileButton
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.None
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.viewmodels.BaseFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class BaseFragment : Fragment(), CoroutineScope, HasBackstack {

    open var statusBarColor: StatusBarColor = StatusBarColor.Light

    @Inject lateinit var theme: Theme

    @Inject lateinit var chromeCastAnalytics: ChromeCastAnalytics

    private val viewModel: BaseFragmentViewModel by viewModels()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.background == null) {
            view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_04))
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
        profileButton: ProfileButton = ProfileButton.None,
        navigationIcon: NavigationIcon = None,
        onNavigationClick: (() -> Unit)? = null,
        toolbarColors: ToolbarColors? = ToolbarColors.Theme(theme = theme, context = toolbar.context),
    ) {
        toolbar.setup(
            title = title,
            menu = menu,
            chromeCastButton = chromeCastButton,
            profileButton = when (profileButton) {
                is ProfileButton.None -> profileButton
                is ProfileButton.Shown -> {
                    ProfileButton.Shown(
                        onClick = {
                            viewModel.onMenuItemTapped(UR.id.menu_profile)
                            (activity as? FragmentHostListener)?.openProfile()
                        },
                    )
                }
            },
            navigationIcon = navigationIcon,
            onNavigationClick = onNavigationClick,
            activity = activity,
            theme = theme,
            toolbarColors = toolbarColors,
        )
        if (toolbarColors != null) {
            updateStatusBarColor(toolbarColors.backgroundColor)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { state ->
                    if (profileButton is ProfileButton.Shown) {
                        toolbar.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                toolbar.updateProfileMenuImage(state.signInState)
                                toolbar.updateProfileMenuBadge(state.showBadgeOnProfileMenu)
                                toolbar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                }
            }
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
