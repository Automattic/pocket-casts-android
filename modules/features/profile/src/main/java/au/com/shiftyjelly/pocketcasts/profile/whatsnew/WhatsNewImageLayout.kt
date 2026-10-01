package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.min

internal object WhatsNewImageLayout {
    private const val HEIGHT_FRACTION = 0.52f

    fun maximumHeight(pageHeight: Dp) = pageHeight * HEIGHT_FRACTION

    fun size(aspectRatio: Float, contentWidth: Dp, pageHeight: Dp): DpSize {
        val height = min(maximumHeight(pageHeight), contentWidth / aspectRatio)
        return DpSize(width = height * aspectRatio, height = height)
    }
}
