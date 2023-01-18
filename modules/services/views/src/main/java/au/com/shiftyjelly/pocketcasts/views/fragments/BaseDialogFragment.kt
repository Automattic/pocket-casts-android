package au.com.shiftyjelly.pocketcasts.views.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.navigation.NavHostController
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
open class BaseDialogFragment : BottomSheetDialogFragment(), CoroutineScope {

    open val statusBarColor: StatusBarColor? = StatusBarColor.Light

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
        if (view.background == null) {
            view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        }
        view.isClickable = true

        val activity = activity
        val statusBarColor = statusBarColor
        if (activity != null && statusBarColor != null) {
            theme.updateWindowStatusBar(window = activity.window, statusBarColor = statusBarColor, context = activity)
        }

        view.doOnLayout {
            ensureExpanded()
        }

        isBeingDragged = false
        addDismissCallback()
    }

    private fun addDismissCallback() {
        val dialog = dialog as BottomSheetDialog
        (dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.addBottomSheetCallback(dismissCallback)
        }
    }

    private fun removeDismissCallback() {
        val dialog = dialog as BottomSheetDialog
        (dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?)?.let { bottomSheet ->
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

        val dialog = dialog as BottomSheetDialog
        (dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            behavior.skipCollapsed = true
        }
    }

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
}
