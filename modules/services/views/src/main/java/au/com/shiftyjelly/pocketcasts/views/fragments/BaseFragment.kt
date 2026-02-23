package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.None
import au.com.shiftyjelly.pocketcasts.views.helper.PredictiveBackAnimator
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class BaseFragment :
    Fragment(),
    CoroutineScope,
    HasBackstack {

    open var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Theme
    open var backgroundTransparent: Boolean = false

    @Inject lateinit var theme: Theme

    @Inject lateinit var chromeCastAnalytics: ChromeCastAnalytics

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(getBackstackCount() > 0) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                onBackGestureStarted(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (enableDefaultBackAnimation()) {
                    view?.let { PredictiveBackAnimator.applyProgress(it, backEvent.progress, scaleAmount = 0.05f, alphaAmount = 0.2f) }
                }
                onBackGestureProgressed(backEvent)
            }

            override fun handleOnBackPressed() {
                if (enableDefaultBackAnimation()) {
                    view?.let {
                        PredictiveBackAnimator.animateToEnd(
                            view = it,
                            targetScale = 0.9f,
                            targetAlpha = 0.7f,
                            duration = PredictiveBackAnimator.Defaults.SHORT_ANIMATION_DURATION_MS,
                        ) {
                            if (isAdded) {
                                performBackNavigation()
                            }
                        }
                    } ?: performBackNavigation()
                } else {
                    performBackNavigation()
                }
            }

            override fun handleOnBackCancelled() {
                if (enableDefaultBackAnimation()) {
                    view?.let { PredictiveBackAnimator.reset(it) }
                }
            }

            private fun performBackNavigation() {
                val handled = onBackPressed()
                isEnabled = handled && getBackstackCount() > 0
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        backPressedCallback = callback
    }

    override fun onDetach() {
        super.onDetach()
        backPressedCallback?.remove()
        backPressedCallback = null
    }

    /**
     * Call whenever the internal backstack changes so the [OnBackPressedCallback] enabled state
     * stays in sync. Required for predictive back gesture support.
     */
    protected fun notifyBackstackChanged() {
        backPressedCallback?.isEnabled = getBackstackCount() > 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.background == null && !backgroundTransparent) {
            view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        }
        view.isClickable = true
        view.isFocusable = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any ongoing back animations to prevent crashes
        view?.let { PredictiveBackAnimator.reset(it) }
    }

    override fun onResume() {
        super.onResume()
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
            theme.updateWindowStatusBarIcons(window = activity.window, statusBarIconColor = statusBarIconColor)
        }
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
            updateStatusBar()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        context?.let {
            menu.tintIcons(it.getThemeColor(UR.attr.secondary_icon_01))
        }
    }

    override fun onBackPressed(): Boolean {
        val childrenWithBackstack: List<HasBackstack> = childFragmentManager.fragments.filterIsInstance<HasBackstack>()
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
            notifyBackstackChanged()
            return true
        }

        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }

    /**
     * Override to enable/disable default predictive back animation.
     * Default: true (animations enabled)
     *
     * Return false if you want to implement custom animations via
     * [onBackGestureStarted] and [onBackGestureProgressed].
     */
    protected open fun enableDefaultBackAnimation(): Boolean = true

    /**
     * Called when a predictive back gesture is started.
     * Override to implement custom animation setup.
     *
     * @param backEvent Contains information about the back gesture (touch position, swipe edge)
     */
    protected open fun onBackGestureStarted(backEvent: BackEventCompat) {
        // Override in subclasses for custom behavior
    }

    /**
     * Called as a predictive back gesture progresses.
     * Override to implement custom animations based on gesture progress.
     *
     * If [enableDefaultBackAnimation] returns true, the default scale/fade animation
     * will be applied before this is called.
     *
     * @param backEvent Contains progress (0.0 to 1.0) and other gesture information
     */
    protected open fun onBackGestureProgressed(backEvent: BackEventCompat) {
        // Override in subclasses for custom behavior
    }
}
