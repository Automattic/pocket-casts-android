package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import androidx.compose.ui.util.lerp
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

class OffsettingBottomSheetCallback(bottomSheetView: View) : BottomSheetCallback() {
    // The bottom sheet, when collapsed, slightly peeks over our content at the bottom.
    // I'm not entirely sure about the root cause, but as far as I can tell, it seems to be
    // related to edge-to-edge interaction with the Material library. I haven't found a way
    // to customize or fix it.
    //
    // As a workaround, adding an arbitrary offset of 100dp to the bottom sheet helps by
    // pushing the content further down when it should be hidden. 100dp is an arbitrary value
    // big enough to hide the content.
    val slideTranslation = 100.dpToPx(bottomSheetView.context).toFloat()

    init {
        // Post initial offset so we account for expanded or collapsed state
        bottomSheetView.post {
            val behavior = BottomSheetBehavior.from(bottomSheetView)
            bottomSheetView.translationY = calculateSlideTranslation(behavior.calculateSlideOffset())
        }
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) = Unit

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        bottomSheet.translationY = calculateSlideTranslation(slideOffset)
    }

    private fun calculateSlideTranslation(slideOffset: Float): Float {
        return lerp(slideTranslation, 0f, slideOffset).coerceIn(0f, slideTranslation)
    }
}
