package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import au.com.shiftyjelly.pocketcasts.ui.R as UR

/**
 * Sets the background and nav bar color to a dynamic color.
 * Must be called after onActivityCreated or else no color will be applied.
 */
fun BottomSheetDialogFragment.applyColor(theme: Theme, @ColorInt color: Int) {
    val dialogBackground = GradientDrawable()
    dialogBackground.color = ColorStateList.valueOf(color)
    dialogBackground.cornerRadius = context?.resources?.getDimension(UR.dimen.bottom_sheet_corner_radius) ?: 0f
    (view?.parent as? View)?.background = dialogBackground
    view?.setBackgroundColor(color)

    dialog?.window?.let { window ->
        theme.setNavigationBarColor(window, true, color)
    }
}
