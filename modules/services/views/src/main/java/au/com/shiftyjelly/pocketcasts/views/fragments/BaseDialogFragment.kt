package au.com.shiftyjelly.pocketcasts.views.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.navigation.NavHostController
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.NavigationBarColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setSystemWindowInsetToPadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@AndroidEntryPoint
open class BaseDialogFragment : BottomSheetDialogFragment(), CoroutineScope {

    open val statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Theme
    open val navigationBarColor: NavigationBarColor = NavigationBarColor.Theme

    private var isBeingDragged = false
    private val dismissCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            isBeingDragged = true
        }
    }

    @Inject lateinit var theme: Theme

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isClickable = true

        dialog?.window?.let { window ->
            theme.updateWindowStatusBarIcons(window = window, statusBarIconColor = statusBarIconColor)
            theme.updateWindowNavigationBarColor(window = window, navigationBarColor = navigationBarColor)
        }

        view.doOnLayout {
            ensureExpanded()
        }

        // add padding to the bottom of the dialog for the navigation bar
        view.setSystemWindowInsetToPadding(bottom = true)

        isBeingDragged = false
        addDismissCallback()
    }

    private fun addDismissCallback() {
        val dialog = dialog as? BottomSheetDialog
        (dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout?)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.addBottomSheetCallback(dismissCallback)
        }
    }

    private fun removeDismissCallback() {
        bottomSheetView()?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.removeBottomSheetCallback(dismissCallback)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        removeDismissCallback()
        (activity as? FragmentHostListener)?.updateStatusBar()
    }

    fun ensureExpanded() {
        // skip this when the user is dragging the bottomsheet
        // as it causes the bottomsheet flicker to the expanded state
        if (isBeingDragged) return

        bottomSheetView()?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            behavior.skipCollapsed = true
        }
    }

    protected fun bottomSheetView() = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

    protected fun addNavControllerToBackStack(loadNavController: () -> NavHostController?, initialRoute: String): Dialog {
        return object : BottomSheetDialog(requireContext(), getTheme()) {
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                val navController = loadNavController()
                if (navController == null || navController.currentDestination?.route == initialRoute) {
                    @Suppress("DEPRECATION")
                    super.onBackPressed()
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    protected fun setDialogTint(
        @ColorInt color: Int,
    ) {
        setStatusBarTint(color)
        setNavigationBarTint(color)
        setBackgroundTint(color)
    }

    protected fun setStatusBarTint(
        @ColorInt color: Int,
    ) {
        requireActivity().window?.let { activityWindow ->
            WindowInsetsControllerCompat(activityWindow, activityWindow.decorView).isAppearanceLightStatusBars = ColorUtils.calculateLuminance(color) > 0.5f
        }
    }

    protected fun setNavigationBarTint(
        @ColorInt color: Int,
    ) {
        requireDialog().window?.let { dialogWindow ->
            WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(color) > 0.5f
        }
    }

    protected fun setBackgroundTint(
        @ColorInt color: Int,
    ) {
        bottomSheetView()?.backgroundTintList = ColorStateList.valueOf(color)
    }
}
