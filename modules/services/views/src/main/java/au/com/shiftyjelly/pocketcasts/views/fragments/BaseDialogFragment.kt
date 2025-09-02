package au.com.shiftyjelly.pocketcasts.views.fragments

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.NavigationBarColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.extensions.setSystemWindowInsetToPadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.ViewPager2AwareBottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
open class BaseDialogFragment : BottomSheetDialogFragment() {

    open val statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Theme
    open val navigationBarColor: NavigationBarColor = NavigationBarColor.Theme
    open val includeNavigationBarPadding: Boolean = true

    private var isBeingDragged = false
    private val dismissCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            isBeingDragged = true
        }
    }

    @Inject
    lateinit var theme: Theme

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
        if (includeNavigationBarPadding) {
            view.setSystemWindowInsetToPadding(bottom = true)
        }

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
            behavior.peekHeight = 0
            behavior.skipCollapsed = true
        }
    }

    protected fun bottomSheetView() = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

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

    protected fun setPreFlingThreshold(thresholdDp: Int) {
        bottomSheetView()
            ?.let { BottomSheetBehavior.from(it) as? ViewPager2AwareBottomSheetBehavior }
            ?.let { behavior ->
                behavior.setPreFlingInterceptor(
                    object : ViewPager2AwareBottomSheetBehavior.PreFlingInterceptor {
                        override fun shouldInterceptFlingGesture(velocityX: Float, velocityY: Float): Boolean {
                            val view = view ?: return false
                            val offsetPx = (view.height * (1f - behavior.calculateSlideOffset())).roundToInt()
                            val offsetDp = offsetPx.pxToDp(requireContext())
                            return offsetDp < thresholdDp
                        }

                        override fun onFlingIntercepted(velocityX: Float, velocityY: Float) {
                            view?.post {
                                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                            }
                        }
                    },
                )
            }
    }

    @Suppress("ktlint:compose:modifier-not-used-at-root")
    @Composable
    protected fun DialogBox(
        modifier: Modifier = Modifier,
        useThemeBackground: Boolean = true,
        fillMaxHeight: Boolean = true,
        themeType: Theme.ThemeType = theme.activeTheme,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = Modifier
                .then(if (fillMaxHeight) Modifier.fillMaxHeight(0.93f) else Modifier)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            Background(themeType, useThemeBackground) {
                val color = MaterialTheme.theme.colors.primaryUi01
                LaunchedEffect(color) {
                    setDialogTint(color.toArgb())
                }
                DialogContent(modifier, content)
            }
        }
        LaunchedEffect(Unit) {
            bottomSheetView()?.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_dialog_fragment)
        }
    }

    @Composable
    private fun DialogContent(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        Box(
            modifier = modifier.windowInsetsPadding(insets),
            content = content,
        )
    }

    @Composable
    private fun Background(
        themeType: Theme.ThemeType,
        useThemeBackground: Boolean,
        content: @Composable () -> Unit,
    ) {
        if (useThemeBackground) {
            AppThemeWithBackground(themeType, content)
        } else {
            AppTheme(themeType, content)
        }
    }
}
